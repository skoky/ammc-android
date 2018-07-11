package com.skoky.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v7.util.SortedList
import android.util.Log
import com.skoky.NetworkBroadcastHandler
import eu.plib.Parser
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer
import javax.sql.DataSource

data class Decoder(val id: String, var ipAddress: String? = null, var decoderType: String? = null, var connection: Socket? = null) {
    override fun equals(other: Any?): Boolean {
        return id == (other as? Decoder)?.id
    }
}

class DecoderService : Service() {

    private var decoders = mutableListOf<Decoder>(Decoder(id = "1111", decoderType = "TranX", ipAddress = "10.0.0.10"))

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Created")

        decoders.sortedWith(compareBy({ it.id }, { it.id }))
    }

    fun listenOnDecodersBroadcasts() {

        doAsync {
            Thread.sleep(2000)
            sendBroadcast()

            NetworkBroadcastHandler.receiveBroadcastData() { processMsg(it) }
        }
    }

    fun getDecoders(): List<Decoder> {
        return decoders.toList()
    }

    fun isDecoderConnected(): Boolean {
        return decoders.any { it.connection != null }
    }

    fun connectOrDisconnectFirstDecoder() {
        if (decoders.isEmpty()) return

        val decoder = decoders.first()

        if (decoder.connection == null) {
            doAsync {
                try {
                    val socket = Socket(decoder.ipAddress, 5403)
                    decoder.connection = socket
                    decoders.remove(decoder)
                    decoders.add(decoder)
                    Log.i(TAG, "Decoder $decoder connected")
                    sendBroadcast()

                    doAsync {
                        listenOnConnection(socket, decoder)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting decoder", e)
                    uiThread {
                        toast("Connection not possible")
                    }
                }
            }
        } else {
            try {
                decoder.connection?.let { it.close() }
                decoder.connection = null
                decoders.remove(decoder)
                decoders.add(decoder)

                sendBroadcast()
            } catch (e: Exception) {
                Log.w(TAG, "Unable to disconnect decoder $decoder", e)
            }
        }
    }

    private fun listenOnConnection(socket: Socket, decoder: Decoder) {
        val buffer = ByteArray(1024)

        try {
            var read = 0
            while (socket.isBound && read != -1) {
                socket.getInputStream()?.let {
                    read = it.read(buffer)
                    Log.i(TAG, "Received $read bytes")
                    val json = JSONObject(Parser.decode(buffer.copyOf(read)))
                    when (json.get("recordType")) {
                        "Passing" -> sendBroadcastPassing(json.toString())
                    }
                }
                sendBroadcast()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Decoder connection error $decoder", e)
            sendBroadcastDisconnected()
        }

        decoder.connection = null
        decoders.remove(decoder)
        decoders.add(decoder)

        Log.i(TAG, "Decoder disconnected")
        sendBroadcast()

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun processMsg(msgB: ByteArray) {
        Log.w(TAG, "Data received: ${msgB.size}")
        val msg = Parser.decode(msgB)
        val json = JSONObject(msg)

        var decoderId: String? = null
        if (json.has("decoderId")) {
            decoderId = json.get("decoderId") as String
            val d = Decoder(decoderId)
            if (!decoders.contains(d)) decoders.add(d)
        }

        if (json.has("recordType") && decoderId != null) when (json.get("recordType")) {
            "Status" -> {
                sendNetworkRequest()
                sendVersionRequest()
            }
            "NetworkSettings" ->
                if (json.has("activeIPAddress") && decoderId != null) {
                    val decoder = decoders.find { it.id == decoderId }
                    decoder?.let { d ->
                        d.ipAddress = json.get("activeIPAddress") as? String
                        decoders.removeIf { it.id == d.id }
                        decoders.add(d)
                    }
                }
            "Version" ->
                if (json.has("decoderType") && decoderId != null) {
                    val decoder = decoders.find { it.id == decoderId }
                    decoder?.let { d ->
                        d.decoderType = json.get("decoderType") as? String
                        decoders.removeIf { it.id == d.id }
                        decoders.add(d)
                    }

                }
        }
        Log.i(TAG, "Decoders: $decoders")
        sendBroadcast()
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

    private fun sendBroadcast() {
        val intent = Intent()
        intent.action = DECODER_REFRESH
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
        return true
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): DecoderService {
            return this@DecoderService
        }
    }

    companion object {
        private const val TAG = "DecoderService"
        val DECODER_REFRESH = "com.skoky.decoder.broadcast"
        val DECODER_PASSING = "com.skoky.decoder.broadcast.passing"
        val DECODER_DISCONNECTED = "com.skoky.decoder.broadcast.disconnected"
    }
}
