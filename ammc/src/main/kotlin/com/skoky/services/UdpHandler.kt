package com.skoky.services

import android.content.Context
import android.util.Log
import com.skoky.AmmcBridge
import com.skoky.CloudDB
import com.skoky.MyApp
import com.skoky.Tools
import com.skoky.Tools.decodeHex
import com.skoky.Tools.toHexString
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

private const val TAG = "DecoderServiceUdp"


fun processUdpMsg(
    app: MyApp,
    context: Context,
    decoderConnector: DecoderConnector,
    msgB: ByteArray
) {
    Log.d(TAG, "Data received: ${msgB.size}")
    val parser = AmmcBridge()
    val msg = parser.p3_to_json_local(msgB.toHexString())
    Log.i(TAG, "HEX String {msg}")
    val json = JSONObject(msg)
    Log.d(TAG, ">> $json")

    if (msg.contains("Error")) {
        CloudDB.badMessageReport(
            app,
            "tcp_msg_with_error",
            msgB.toHexString()
        )
    }

    val decoderId: String?
    if (json.has("decoder_id")) {
        decoderId = json.get("decoder_id") as String
    } else {
        Log.w(TAG, "Received P3 message without decoderId. Wired! $json")
        return
    }

    var decoder = decoderConnector.getAllDecoders().find { it.decoderId == decoderId }
    if (decoder == null) {
        decoder = Decoder.newDecoder(decoderId = decoderId)
        decoderConnector.getAllDecoders().addOrUpdate(decoder)
    }

    decoder.lastSeen = System.currentTimeMillis()

    decoder.let { d ->

        if (json.has("msg")) when (json.get("msg")) {
            "STATUS" -> {
                sendUdpNetworkRequest()
                sendUdpVersionRequest()
                sendBroadcastData(d, json, context)
                decoderConnector.getAllDecoders().addOrUpdate(decoder)
            }

            "NETWORKSETTINGS" ->
                if (json.has("activeIPAddress")) {  // FIXME naming?
                    val ipAddress = json.get("activeIPAddress") as? String
                    decoderConnector.getAllDecoders()
                        .addOrUpdate(decoder.copy(ipAddress = ipAddress))
                    sendBroadcastData(d, json, context)
                }

            "VERSION" ->
                if (json.has("decoder_type")) {
                    val decoderType = json.get("decoder_type") as? String
                    decoderConnector.getAllDecoders()
                        .addOrUpdate(decoder.copy(decoderType = decoderType))
                    sendBroadcastData(d, json, context)
                }

            "ERROR" ->
                CloudDB.badMessageReport(
                    app,
                    "tcp_error",
                    msgB.toHexString()
                )

        } else {
            Log.w(TAG, "Msg with record type on UDP. Wired! $json")
        }
        sendBroadcastDecodersUpdate(context)
    }
}

fun sendUdpNetworkRequest() {
    sendUdpBroadcastMessage("{\"msg\":\"NETWORKSETTINGS\",\"empty_fields\":[\"activeIPAddress\"]}")
}

private fun sendUdpVersionRequest() {
    sendUdpBroadcastMessage("{\"msg\":\"VERSION\",\"empty_fields\":[\"decoderType\"]}")
}

private fun sendUdpBroadcastMessage(msg: String) {
    try {
        DatagramSocket(Tools.P3_DEF_PORT).use { socket ->
            socket.broadcast = true
            socket.connect(InetAddress.getByName("255.255.255.255"), Tools.P3_DEF_PORT)
            val parser = AmmcBridge()
            val bytes = parser.encode_local(msg)
            val bytes2 = bytes.decodeHex()
            Log.d(TAG, "Bytes size ${bytes2.size}")
            socket.send(DatagramPacket(bytes2, bytes2.size))
        }
    } catch (e: java.lang.Exception) {
        Log.w(TAG, "Error $e", e)
    }
}