package com.skoky.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class DecodeBroadcastReceiver() : BroadcastReceiver() {


    private var handler: (ByteArray) -> Unit = {}

    fun setHandler(h: (ByteArray) -> Unit) {
        this.handler = h
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { i ->
            val data = i.getByteArrayExtra("data")
            data?.let { d ->
                Log.w(TAG, "Received something ${d.size}")
                handler(d)
            }
        }
    }

    companion object {
        private const val TAG = "DecodeBroadcastReceiver"
    }
}
