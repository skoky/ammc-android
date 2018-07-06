package com.skoky.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.skoky.NetworkBroadcastHandler
import org.jetbrains.anko.doAsync


class DecoderService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Created")
    }

    fun listenOnDecodersBroadcasts() {

        doAsync {
            NetworkBroadcastHandler.receiveBroadcastData(applicationContext)
        }

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
