package com.skoky.services

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.skoky.AmmcBridge
import com.skoky.MyApp
import com.skoky.Tools.decodeHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.schedule

private const val TAG = "DecoderServiceConnector"

class DecoderConnector(appContext: Context, private var app: MyApp) {

    // Decoder(id = "1111", decoderType = "TranX", ipAddress = "10.0.0.10"))
    private var decoders = mutableListOf<Decoder>()
    private var context: Context = appContext;

    init {
        decoderAutoCleanup()
    }


    private fun updateDecoder(decoder: Decoder, socket: Socket) {

        if (!isP3Decoder(decoder)) {
            decoders.addOrUpdate(
                decoder.copy(
                    decoderId = vostokDecoderId(
                        decoder.ipAddress,
                        decoder.port
                    ), connection = socket, lastSeen = System.currentTimeMillis()
                )
            )
        } else if (isP3Decoder(decoder)) {
            decoders.addOrUpdate(
                decoder.copy(
                    connection = socket,
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
    }

    //    @RequiresApi(Build.VERSION_CODES.N)
    fun decoderAutoCleanup() {
        Timer().schedule(1000, 1000) {
            // removes inactive decoders
            decoders.removeIf { d ->
                Log.i(
                    TAG,
                    "Decoder $d diff: ${System.currentTimeMillis() - d.lastSeen}"
                )
                if ((System.currentTimeMillis() - d.lastSeen) > INACTIVE_DECODER_TIMEOUT) {
                    Log.i(TAG, "Removing decoder $d, current decoders $decoders")
                    if (d.connection != null) {
                        d.connection?.close()
                        sendBroadcastDisconnected(d, context)
                    }
                    sendBroadcastDecodersUpdate(context)
                    true
                } else {
                    false
                }
            }
        }
    }


    fun exploreDecoder(uuid: UUID) {
        val socket = decoders.find { it.uuid == uuid }?.connection

        CoroutineScope(Dispatchers.IO).launch {
            socket?.let { s ->
                if (s.isConnected) {

                    exploreMessages.forEach { m ->
                        try {
                            val parser = AmmcBridge()
                            val parsed = parser.encode_local(m)
                            parsed.let { p ->
                                if (p.isNotEmpty()) {
                                    s.getOutputStream().write(p.decodeHex())
                                }
                            }
                        } finally {
                            Thread.sleep(200)
                        }
                    }
                }
            }
        }

    }


    fun connectDecoder(address: String, notifyError: Boolean = true): Boolean {
        Log.d(TAG, "Connecting to $address $notifyError")

        var addressIp: String = ""
        var port: Int? = null

        val foundByIp: Decoder? =
            if (address.contains(":") && address.split(":").size == 2) {
                val fields = address.split(":")
                addressIp = fields[0]
                port = fields[1].toInt()
                decoders.find { it.ipAddress == addressIp && it.port == port }

            } else {
                decoders.find { it.ipAddress == address }
            }

        if (foundByIp != null) {
            connectDecoderParsed(foundByIp)
        } else {  // create new decoder
            //toast("Connecting to $addressIp:$port")
            if (port != null) {
                connectDecoderParsed(
                    Decoder.newDecoder(ipAddress = addressIp, port = port),
                    notifyError
                )
            } else {
                connectDecoderParsed(
                    Decoder.newDecoder(ipAddress = address),
                    notifyError
                )
            }
        }
        sendBroadcastDecodersUpdate(context)
        return true
    }


    fun connectDecoderParsed(decoder: Decoder, notifyError: Boolean = true) {

        if (decoder.connection == null || (!decoder.connection!!.isConnected)) {
            CoroutineScope(Dispatchers.IO).launch {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(decoder.ipAddress, decoder.port ?: 5403), 5000)

                    updateDecoder(decoder, socket)

                    Log.i(TAG, "Decoder $decoder connected")
                    sendBroadcastConnect(decoder, context)

                    CoroutineScope(Dispatchers.IO).launch {
                        listenOnSocketConnection(socket, decoder)
                    }

                    sendInitialVersionRequest(decoder, socket)
                } catch (e: Exception) {

                    socket.close()
                    if (notifyError) {
                        Log.e(TAG, "Error connecting decoder", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Connection not possible to ${decoder.ipAddress}:${decoder.port}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        } else {
            Log.e(TAG, "Decoder already connected $decoder")
        }
    }


    private fun listenOnSocketConnection(socket: Socket, orgDecoder: Decoder) {
        val buffer = ByteArray(1024)
        var decoder = orgDecoder
        try {
            var read = 0
            while (socket.isConnected && read != -1) {
                socket.getInputStream()?.let {
                    read = it.read(buffer)
                    Log.i(TAG, "Received $read bytes")
                    if (read > 0) {
                        val json = processTcpMsg(
                            app,
                            buffer.copyOf(read),
                            vostokDecoderId(decoder.ipAddress, decoder.port)
                        )
                        Log.d(TAG, "JSON: $json")
                        if (json.get("msg").toString().isNotEmpty()) sendBroadcastData(
                            decoder,
                            json, context
                        )
                        handleMessage(app, context, json, decoder, buffer, decoders)

                        if (json.has("decoder_type")) {
                            json.getString("decoder_type")
                                .let { type ->
                                    decoders.addOrUpdate(
                                        decoder.copy(
                                            decoderType = type
                                        )
                                    )
                                }
                        } else {
                            if (json.has("decoder_type")) {
                                json.getString("decoder_type")
                                    .let { type ->
                                        decoders.addOrUpdate(
                                            decoder.copy(
                                                decoderType = type
                                            )
                                        )
                                    }
                            }
                        }
                        decoders.addOrUpdate(decoder.copy(lastSeen = System.currentTimeMillis()))
                        sendBroadcastDecodersUpdate(context)
                    }
                }
                decoders.find { it.uuid == decoder.uuid }?.let { decoder = it }
            }
            Log.i(TAG, "Connected ${socket.isConnected}, read $read")
        } catch (e: Exception) {
            Log.w(TAG, "Decoder connection error $decoder", e)
        } catch (t: Throwable) {
            Log.e(TAG, "Decoder connection throwable $decoder", t)
        } finally {
            decoder.connection?.close()
            decoder.connection = null
            Log.i(TAG, "Decoder disconnected")
            sendBroadcastDisconnected(decoder, context)
            decoders.remove(orgDecoder)
        }
    }


    fun getConnectedDecoder() =
        decoders.find { d -> d.connection != null && d.connection!!.isConnected }

    fun getAllDecoders() = decoders

    fun getBestFreeDecoder(): Decoder? = decoders.maxByOrNull { it.lastSeen }

    fun isDecoderConnected() = decoders.any { it.connection != null }

    fun disconnectAllDecoders() {
        getConnectedDecoder()?.connection?.let { c ->
            if (c.isConnected) {
                try {
                    c.close()
                } catch (e: Exception) {
                    Log.i(
                        TAG,
                        "Disconnection issue for decoder ${getConnectedDecoder()}, error $e"
                    )
                }
            }
            getConnectedDecoder()?.connection = null
        }
    }


    fun connectDecoderByUUID(decoderUUIDString: String) {
        val uuid = UUID.fromString(decoderUUIDString)
        val found = decoders.find { it.uuid == uuid }
        found?.let { connectDecoderParsed(it) }
    }

    companion object {
        private const val INACTIVE_DECODER_TIMEOUT: Long = 10000  // 10secs
    }
}

fun MutableList<Decoder>.addOrUpdate(decoder: Decoder) {

    var found = false

    this.forEach { d ->
        if (d.uuid == decoder.uuid) {
            if (decoder.decoderId != d.decoderId)
                Log.i("D", "DecodersX: decoder ID to update ${decoder.decoderId} -> ${d.decoderId}")
            decoder.decoderId?.let { d.decoderId = it }
            decoder.ipAddress?.let { d.ipAddress = it }
            decoder.port?.let { d.port = it }
            decoder.decoderType?.let { d.decoderType = it }
            decoder.connection?.let { d.connection = it }
            if (decoder.lastSeen > d.lastSeen) d.lastSeen = decoder.lastSeen
            found = true
        }
    }
    if (!found) {
        decoder.lastSeen = System.currentTimeMillis()
        this.add(decoder)
    }
}



