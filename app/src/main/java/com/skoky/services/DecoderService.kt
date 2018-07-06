package com.skoky.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.skoky.NetworkBroadcastHandler
import eu.plib.Parser
import org.jetbrains.anko.doAsync
import org.json.JSONObject

data class Decoder(val id: String) {
    override fun equals(other: Any?): Boolean {
        return id == (other as? Decoder)?.id
    }
}

class DecoderService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Created")
    }

    private val decoders = mutableListOf<Decoder>()

    fun listenOnDecodersBroadcasts() {

        doAsync {
            NetworkBroadcastHandler.receiveBroadcastData(applicationContext) { processMsg(it) }
        }
    }

    fun getDecoders(): List<Decoder> {
        return decoders.toList()
    }

    private fun processMsg(msgB: ByteArray) {
        Log.w(TAG,"Data received: ${msgB.size}")
        val msg = Parser.decode(msgB)
        val json = JSONObject(msg)
        if (json.has("decoderId")) {
            val decoderId = json.get("decoderId") as String
            val d = Decoder(decoderId)
            if (!decoders.contains(d)) decoders.add(d)
            sendBroadcast()
        }
    }

    private fun sendBroadcast() {
        val intent = Intent()
        intent.action = "com.skoky.decoder.broadcast"
        applicationContext.sendBroadcast(intent)
        Log.w(TAG,"Broadcast sent $intent")

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
    }
}
