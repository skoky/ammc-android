package com.skoky.services

import android.util.Log
import com.skoky.AmmcBridge
import com.skoky.CloudDB
import com.skoky.MyApp
import com.skoky.P98Parser
import com.skoky.Tools
import com.skoky.Tools.decodeHex
import com.skoky.Tools.toHexString
import com.skoky.VOSTOK_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

private const val TAG = "DecoderServiceHelper"

fun sendInitialVersionRequest(decoder: Decoder, socket: Socket) {
    if (isP3Decoder(decoder)) {
        val parser = AmmcBridge()
        val versionRequest =
            parser.encode_local("{\"msg\":\"Version\",\"empty_fields\":[\"decoderType\"]}")

        socket.getOutputStream().write(versionRequest.decodeHex())
    }
}

fun isP3Decoder(d: Decoder): Boolean {
    return d.port == Tools.P3_DEF_PORT
}

fun processTcpMsg(
    app: MyApp,
    msgImut: ByteArray,
    decoderIdVostok: String?
): JSONObject {
    val parser = AmmcBridge()
    var msg2 = msgImut.toMutableList()
    if (msg2[0] == 0x01.toByte()) {
        val m = msg2.toMutableList()
        m.removeAt(0)
        msg2 = m.toMutableList()
    }
    val msg = msg2.toByteArray()
    return if (msg.size > 1 && msg[0] == 0x8e.toByte()) {
        val responses = JSONArray(parser.p3_to_json_local(msg.toHexString()))
        Log.i(TAG, "response $responses")
        if (responses.length() > 0) {
            return responses.get(0) as JSONObject // FIXME get all messages
        }
        JSONObject("{\"msg\":\"Error\",\"description\":\"No message\"}")
    } else if (msg.size > 1 && (msg[0] == '@'.code.toByte() || msg[0] == '#'.code.toByte())) {
        JSONObject(P98Parser.parse(msg, decoderIdVostok ?: "-"))
    } else {
        Log.w(TAG, "Invalid msg on TCP " + msg.toHexString())
        Log.w(TAG, "Invalid msg on TCP $msg")
        CloudDB.badMessageReport(
            app,
            "tcp_msg_error",
            msg.toHexString()
        )
        JSONObject("{\"msg\":\"Error\",\"description\":\"Invalid message\"}")
    }
}


fun checkTcpSocket(ipaddress: String, port: Int): Boolean {
    Socket().use { socket ->
        return try {
            socket.connect(InetSocketAddress(ipaddress, port), 5000)
            socket.isConnected
        } catch (e: java.lang.Exception) {
            false
        }
    }
}


fun appendDriver(app: MyApp, json: JSONObject) {

    val transponder = if (json.has("tran_code"))
        json.getString("tran_code")
    else if (json.has("transponder"))
        json.getString("transponder")
    else if (json.has("transponder_code"))
        json.getString("transponder_code")
    else if (json.has("driver_id"))
        json.getString("driver_id")
    else null

    transponder?.let {
        app.recentTransponders.add(it)
    }
}


