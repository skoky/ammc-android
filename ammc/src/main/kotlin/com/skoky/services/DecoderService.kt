package com.skoky.services

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.skoky.*
import com.skoky.Tools.P3_DEF_PORT
import com.skoky.Tools.decodeHex
import com.skoky.Tools.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Thread.sleep
import java.net.*
import java.util.*
import kotlin.concurrent.schedule

const val VOSTOK_DEFAULT_IP = "10.10.100.254"
const val VOSTOK_DEFAULT_PORT = 8899


class DecoderService : Service() {

    private lateinit var decoderConnector: DecoderConnector
    private lateinit var udpJob: Job
    private lateinit var vostokJob: Job
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Created")

        decoderConnector = DecoderConnector(applicationContext, application as MyApp);

        udpJob = CoroutineScope(Dispatchers.IO).launch {
            NetworkBroadcastHandler.receiveBroadcastData {
                processUdpMsg(
                    application as MyApp,
                    applicationContext,
                    decoderConnector,
                    it,
                )
            }
        }

        vostokJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                checkVostok(applicationContext, decoderConnector)
                sleep(5000)
            }
        }
    }



    private val myBinder = MyLocalBinder()
    override fun onDestroy() {
        super.onDestroy()
        Tools.wakeLock(this, false)
        Log.w(TAG, "Destroyed")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.w(TAG, "rebind")
    }

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    fun disconnectAllDecoders() {
        decoderConnector.disconnectAllDecoders()
    }

    fun isDecoderConnected(): Boolean {
        return decoderConnector.isDecoderConnected()
    }

    fun connectDecoderByUUID(uuid: String) {
        decoderConnector.connectDecoderByUUID(uuid)
    }

    fun getDecoders(): List<Decoder> {
        return decoderConnector.getAllDecoders().toList()
    }

    fun connectDecoder(decoderAddress: String): Boolean {
        return decoderConnector.connectDecoder(decoderAddress)
    }

    fun connectDecoderParsed(decoder: Decoder) {
        decoderConnector.connectDecoderParsed(decoder)
    }

    fun isConnectedDecoderVostok(): Boolean {
        return isConnectedDecoderVostok(decoderConnector)
    }

    fun getConnectedDecoder(): Decoder? {
        return decoderConnector.getConnectedDecoder()
    }

    fun exploreDecoder(uuid: UUID) {
        decoderConnector.exploreDecoder(uuid)
    }

    fun getBestFreeDecoder(): Decoder? {
        return decoderConnector.getBestFreeDecoder()
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): DecoderService {
            return this@DecoderService
        }
    }

    companion object {
        private const val TAG = "DecoderService"
        const val DECODERS_UPDATE = "com.skoky.decoder.broadcast.decoders_update"
        const val DECODER_CONNECT = "com.skoky.decoder.broadcast.connect"
        const val DECODER_DATA = "com.skoky.decoder.broadcast.data"
        const val DECODER_PASSING = "com.skoky.decoder.broadcast.passing"
        const val DECODER_DISCONNECTED = "com.skoky.decoder.broadcast.disconnected"

    }
}

