package com.skoky.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.util.Log
import com.skoky.NetworkBroadcastHandler
import eu.plib.Parser
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.net.*
import java.util.*
import kotlin.concurrent.schedule


data class Decoder(var id: String, var ipAddress: String? = null, var decoderType: String? = null,
                   var connection: Socket? = null, var lastSeen: Long? = null) {
    override fun equals(other: Any?): Boolean {
        return id == (other as? Decoder)?.id
    }
}

class DecoderService : Service() {

    private var decoders = mutableListOf<Decoder>() // Decoder(id = "1111", decoderType = "TranX", ipAddress = "10.0.0.10"))

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Created")

        decoders.sortedWith(compareBy({ it.id }, { it.id }))

        Timer().schedule(1000, 1000) {
            // removes inactive decoders
            decoders.removeIf { d ->
                d.lastSeen?.let { ls ->
                    Log.i(TAG, "Decoder $d diff: ${System.currentTimeMillis() - ls}")
                    val toRemove = (System.currentTimeMillis() - ls) > INACTIVE_DECODER_TIMEOUT
                    if (toRemove) {
                        Log.i(TAG, "Removing decoder $d, current decoders $decoders")
                        d.connection?.close()
                    }
                    toRemove != null
                }!!
            }
        }

        doAsync {
            NetworkBroadcastHandler.receiveBroadcastData { processMsg(it) }
        }

    }

    fun getDecoders(): List<Decoder> {
        return decoders.toList()
    }

    fun isDecoderConnected(): Boolean {
        return decoders.any { it.connection != null }
    }

    fun connectOrDisconnectFirstDecoder() {
        if (decoders.isEmpty()) {
            Log.w(TAG, "No decoders, no connection...")
            return
        }
        connectDecoder2(decoders.first())
    }

    fun connectDecoder2(decoder: Decoder) {

        if (decoders.contains(decoder)) {
            Log.e(TAG, "Decoder already registered")
            return
        }
        if (decoder.connection == null) {
            doAsync {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(decoder.ipAddress, 5403), 5000)
                    decoder.connection = socket
                    decoder.lastSeen = System.currentTimeMillis()
                    decoders.add(decoder)
                    Log.i(TAG, "Decoder $decoder connected")
                    sendBroadcastConnect()

                    doAsync {
                        listenOnSocketConnection(socket, decoder)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting decoder", e)
                    socket.close()
                    decoders.remove(decoder)
                    uiThread {
                        toast("Connection not possible to ${decoder.ipAddress}:5403")
                    }
                }
            }
        } else {
            Log.e(TAG, "Decoder connected $decoder")
        }
    }

    fun disconnectDecoder2(decoder: Decoder) {

        try {
            decoder.connection?.let {
                it.close()
                sendBroadcastDisconnected()
            }
            // cleanup
            decoder.connection = null
            decoders.remove(decoder)

        } catch (e: Exception) {
            Log.w(TAG, "Unable to disconnect decoder $decoder", e)
        }
    }


// TODO clean decoders list with non-active decoders

    private fun listenOnSocketConnection(socket: Socket, decoder: Decoder) {
        val buffer = ByteArray(1024)

        try {
            var read = 0
            while (socket.isBound && read != -1) {
                socket.getInputStream()?.let {
                    read = it.read(buffer)
                    Log.i(TAG, "Received $read bytes")
                    if (read > 0) {
                        val json = JSONObject(Parser.decode(buffer.copyOf(read)))
                        when {
                            json.get("recordType").toString() == "Passing" -> sendBroadcastPassing(json.toString())
                            json.get("recordType").toString().isNotEmpty() -> sendBroadcastData(json.toString())
                            else -> Log.w(TAG, "received unknown data $json")
                        }
                        if (decoder.id == "?" && json.has("decoderId")) decoder.id = json.get("decoderId") as String
                        decoders.forEach { if (it.id == decoder.id) it.lastSeen = System.currentTimeMillis() }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Decoder connection error $decoder", e)
        } finally {
            decoder.connection = null
            decoders.remove(decoder)
            Log.i(TAG, "Decoder disconnected")
            sendBroadcastDisconnected()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun processMsg(msgB: ByteArray) {
        Log.w(TAG, "Data received: ${msgB.size}")
        val msg = Parser.decode(msgB)
        val json = JSONObject(msg)

        var decoder: Decoder? = null
        if (json.has("decoderId")) {
            val decoderId = json.get("decoderId") as String
            decoder = Decoder(decoderId)
            if (!decoders.contains(decoder)) decoders.add(decoder)
        }

        decoder?.let {

            if (json.has("recordType")) when (json.get("recordType")) {
                "Status" -> {
                    sendNetworkRequest()
                    sendVersionRequest()
                    sendBroadcastData(json.toString())
                }
                "NetworkSettings" ->
                    if (json.has("activeIPAddress")) {
                        val decoder2 = decoders.find { it.id == decoder.id }
                        decoder2?.let { d ->
                            d.ipAddress = json.get("activeIPAddress") as? String
                            decoders.removeIf { it.id == d.id }
                            decoders.add(d)
                        }
                        sendBroadcastData(json.toString())
                    }
                "Version" ->
                    if (json.has("decoderType")) {
                        val decoder2 = decoders.find { it.id == decoder.id }
                        decoder2?.let { d ->
                            d.decoderType = json.get("decoderType") as? String
                            decoders.removeIf { it.id == d.id }
                            decoders.add(d)
                        }
                        sendBroadcastData(json.toString())
                    }
            }
            Log.i(TAG, "Decoders: $decoders")
        }
    }

    private fun sendVersionRequest() {
        sendBroadcastMessage("{\"recordType\":\"Version\",\"emptyFields\":[\"decoderType\"],\"VERSION\":\"2\"}")
    }

    private fun sendNetworkRequest() {
        sendBroadcastMessage("{\"recordType\":\"NetworkSettings\",\"emptyFields\":[\"activeIPAddress\"],\"VERSION\":\"2\"}")
    }

    private fun sendBroadcastMessage(msg: String) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(5403)
            socket.broadcast = true
            socket.connect(InetAddress.getByName("255.255.255.255"), 5403)
            val bytes = Parser.encode(msg)
            Log.w(TAG, "Bytes size ${bytes.size}")
            socket.send(DatagramPacket(bytes, bytes.size))
        } catch (e: Exception) {
            Log.w(TAG, "Error $e", e)
        } finally {
            socket?.close()
        }
    }

    private fun sendBroadcastConnect() {
        val intent = Intent()
        intent.action = DECODER_CONNECT
        applicationContext.sendBroadcast(intent)
        Log.w(TAG, "Broadcast sent $intent")
    }

    private fun sendBroadcastDisconnected() {
        val intent = Intent()
        intent.action = DECODER_DISCONNECTED
        applicationContext.sendBroadcast(intent)
        Log.w(TAG, "Broadcast sent $intent")
    }

    private fun sendBroadcastPassing(jsonData: String) {
        val intent = Intent()
        intent.action = DECODER_PASSING
        intent.putExtra("Passing", jsonData)
        applicationContext.sendBroadcast(intent)
        Log.w(TAG, "Broadcast passing sent $intent")
    }

    private fun sendBroadcastData(jsonData: String) {
        val intent = Intent()
        intent.action = DECODER_DATA
        intent.putExtra("Data", jsonData)
        applicationContext.sendBroadcast(intent)
        Log.w(TAG, "Broadcast data sent $intent")
    }

    private val myBinder = MyLocalBinder()
    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "Destroyed")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.w(TAG, "rebind")
    }

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    fun connectDecoder(address: String): Boolean {
        Log.d(TAG, "Connecting to $address")

        val probablyDecoder = Decoder("?", address)
        connectDecoder2(probablyDecoder)
        return true
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): DecoderService {
            return this@DecoderService
        }
    }

    companion object {
        private const val TAG = "DecoderService"
        const val DECODER_CONNECT = "com.skoky.decoder.broadcast.decoder_connect"
        const val DECODER_DATA = "com.skoky.decoder.broadcast.data"
        const val DECODER_PASSING = "com.skoky.decoder.broadcast.passing"
        const val DECODER_DISCONNECTED = "com.skoky.decoder.broadcast.disconnected"
        private const val INACTIVE_DECODER_TIMEOUT: Long = 30000  // 10secs
    }
}
