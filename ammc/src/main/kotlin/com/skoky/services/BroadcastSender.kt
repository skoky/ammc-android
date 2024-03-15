package com.skoky.services

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.skoky.AmmcBridge
import com.skoky.Const
import com.skoky.DefaultPrefs
import com.skoky.Tools
import com.skoky.Tools.decodeHex
import com.skoky.services.DecoderService.Companion.DECODERS_UPDATE
import com.skoky.services.DecoderService.Companion.DECODER_CONNECT
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import com.skoky.services.DecoderService.Companion.DECODER_PASSING
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


private const val TAG = "DecoderServiceBroadcastSender"





fun sendBroadcastDecodersUpdate(context: Context) {
    val intent = Intent()
    intent.action = DECODERS_UPDATE
    context.sendBroadcast(intent)
    Log.d(TAG, "Broadcast sent $intent")
}

fun sendBroadcastConnect(decoder: Decoder, context: Context) {
    Tools.wakeLock(context, true)
    val intent = Intent()
    intent.action = DECODER_CONNECT
    intent.putExtra("uuid", decoder.uuid.toString())
    context.sendBroadcast(intent)
    Log.d(TAG, "Broadcast sent $intent")
}

fun sendBroadcastDisconnected(decoder: Decoder, context: Context) {
    Tools.wakeLock(context, false)
    val intent = Intent()
    intent.action = DECODER_DISCONNECTED
    intent.putExtra("uuid", decoder.uuid.toString())
    // TBD more data as it is not in decoders anymore
    context.sendBroadcast(intent)
    Log.d(TAG, "Broadcast sent $intent")
}

fun sendBroadcastPassing(jsonData: String, context: Context) {
    val intent = Intent()
    intent.action = DECODER_PASSING
    intent.putExtra("PASSING", jsonData)
    context.sendBroadcast(intent)
    Log.d(TAG, "Broadcast passing sent $intent")



    if (DefaultPrefs(context).getBoolean(
            Const.transponderSoundK,
            true
        )
    ) {
        ToneGenerator(
            AudioManager.STREAM_MUSIC,
            100
        ).startTone(ToneGenerator.TONE_CDMA_INTERCEPT, 200)
    }
}

fun sendBroadcastData(decoder: Decoder?, jsonData: JSONObject, context: Context) {
    //updateCache(jsonData)
    val intent = Intent()
    intent.action = DECODER_DATA
    intent.putExtra("Data", jsonData.toString())
    decoder?.let { intent.putExtra("uuid", it.toString()) }
    context.sendBroadcast(intent)
    Log.d(TAG, "Broadcast data sent $intent")
}