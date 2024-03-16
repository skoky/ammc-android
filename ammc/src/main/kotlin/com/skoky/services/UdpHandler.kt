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
import java.lang.Exception
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
    val msg = parser.p3_network_to_json_local(msgB.toHexString())
    val spl = msg.split(";")

    try {
        val ip = spl[0]
        val decoderId = spl[1]

        Log.d(TAG, ">> UDP $msg")

        if (msg.contains("Error")) {
            CloudDB.badMessageReport(
                app,
                "tcp_msg_with_error",
                msgB.toHexString()
            )
        }


        var decoder = decoderConnector.getAllDecoders().find { it.decoderId == decoderId }  // fixme
        if (decoder == null) {
            decoder = Decoder.newDecoder(decoderId = decoderId, ipAddress = ip)
            decoderConnector.getAllDecoders().addOrUpdate(decoder)
        }

        decoder.lastSeen = System.currentTimeMillis()

        sendBroadcastDecodersUpdate(context)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected msg received over UDP ${msgB.toHexString()} / $e")
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
            Log.d(TAG, "Bytes send to UDP size ${bytes2.size}")
            socket.send(DatagramPacket(bytes2, bytes2.size))
        }
    } catch (e: java.lang.Exception) {
        Log.w(TAG, "Error $e", e)
    }
}