package com.skoky.services

import android.content.Context
import android.util.Log
import com.skoky.VOSTOK_NAME

private const val TAG = "DecoderServiceVostok"


private fun isVostok(d: Decoder): Boolean {
    return d.decoderType == VOSTOK_NAME || d.decoderId == VOSTOK_NAME
}

fun isConnectedDecoderVostok(decoderConnector: DecoderConnector): Boolean {
    val connectedDecoder = decoderConnector.getConnectedDecoder()
    connectedDecoder?.let {
        return isVostok(it)
    }
    return false
}

fun vostokDecoderId(ipAddress: String?, port: Int?): String? {
    if (ipAddress == null || port == null) return null
    return "$ipAddress:$port"
}


fun checkVostok(context: Context, decoderConnector: DecoderConnector) {
    val connectedDecoder = decoderConnector.getConnectedDecoder()
    val alreadyHaveVostok = decoderConnector.getAllDecoders().find { isVostok(it) }
    if (alreadyHaveVostok == null) {
        if (connectedDecoder == null) {
            Log.d(TAG, "Vostok default connecting....")
            val validSocket = checkTcpSocket(VOSTOK_DEFAULT_IP, VOSTOK_DEFAULT_PORT)
            Log.d(TAG, "Vostok socket $validSocket")
            if (validSocket) {      // default vostok decoder found
                val newDecoder = Decoder.newDecoder(
                    VOSTOK_DEFAULT_IP, VOSTOK_DEFAULT_PORT,
                    vostokDecoderId(VOSTOK_DEFAULT_IP, VOSTOK_DEFAULT_PORT),
                    VOSTOK_NAME
                )
                decoderConnector.getAllDecoders().addOrUpdate(newDecoder)
                sendBroadcastDecodersUpdate(context)
            }
        } else Log.d(TAG, "Vostok: Already connected another decoder")
    } else Log.d(TAG, "Already have vostok")
}