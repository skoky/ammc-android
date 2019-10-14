package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.skoky.CloudDB
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.DecoderService
import org.json.JSONObject


data class Time(val us: Long)

class DataReceiver(val handler: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            handler(it.getStringExtra("Data"))
        }
    }
}

class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        handler()
    }
}

lateinit var disconnectReceiver: ConnectionReceiver
lateinit var connectReceiver: ConnectionReceiver

open class FragmentCommon : Fragment() {

    companion object {
        val TAG = FragmentCommon::class.simpleName
    }

    fun registerConnectionHandlers() {

        disconnectReceiver = ConnectionReceiver {
            Log.i(ConsoleModeFragment.TAG, "Disconnected")
            Toast.makeText(activity, getString(R.string.decoder_not_connected), Toast.LENGTH_LONG).show()
        }
        connectReceiver = ConnectionReceiver {
            Log.i(ConsoleModeFragment.TAG, "Connected")
            Toast.makeText(activity, getString(R.string.decoder_re_connected), Toast.LENGTH_SHORT).show()
        }
        context?.let {
            it.registerReceiver(disconnectReceiver, IntentFilter(DecoderService.DECODER_DISCONNECTED))
            it.registerReceiver(connectReceiver, IntentFilter(DecoderService.DECODER_CONNECT))

        }
    }

    override fun onDetach() {
        super.onDetach()
        try {
            context?.unregisterReceiver(connectReceiver)
            context?.unregisterReceiver(disconnectReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "Receiver not registered")
        }
    }

    fun getTimeFromPassingJson(json: JSONObject): Time {
        return when {
            json.has("RTC_Time") -> Time((json.get("RTC_Time") as String).toLong())
            json.has("UTC_Time") -> Time((json.get("UTC_Time") as String).toLong())
            json.has("msecs_since_start") ->
                Time((json.get("msecs_since_start") as Int).toLong() * 1000)
            else -> {
                Log.w(TrainingModeFragment.TAG, "No time in passing record $json")
                activity?.let {
                    CloudDB.badMessageReport(it.application as MyApp, "passing-no-time", json.toString())
                }
                return Time(0L)
            }
        }
    }

    fun getTransponderFromPassingJson(json: JSONObject): String {

        return when {
            json.has("transponder") -> (json.get("transponder") as Int).toString()
            json.has("transponderCode") -> json.get("transponderCode") as String
            json.has("driverId") -> json.get("driverId") as String
            else -> {
                Log.w(TrainingModeFragment.TAG, "No racer identification in Passing $json")
                json.let {
                    activity?.let { a ->
                        CloudDB.badMessageReport(a.application as MyApp, "passing_not_transponder", it.toString())
                    }
                }
                return "---"
            }
        }
    }
}