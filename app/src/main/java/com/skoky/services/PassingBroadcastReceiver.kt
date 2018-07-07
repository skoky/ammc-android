package com.skoky.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PassingBroadcastReceiver() : BroadcastReceiver() {

    private var handler: (String) -> Unit = {}

    fun setHandler(h: (String) -> Unit) {
        this.handler = h
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { i ->
            val data = i.getStringExtra("Passing")

            data?.let { d ->
                Log.w(TAG, "Received passing ")
                handler(d)
            }
        }
    }

    companion object {
        private const val TAG = "PassingBroadcastReceiver"
    }
}
