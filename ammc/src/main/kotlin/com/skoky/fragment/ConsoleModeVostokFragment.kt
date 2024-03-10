package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.children
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import org.json.JSONObject


class ConsoleModeVostokFragment : FragmentCommon() {

    private lateinit var dataHandler: BroadcastReceiver

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    class DataReceiver(val handler: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                handler(it.getStringExtra("Data")?:"")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_consolemode_list, container, false)

        registerConnectionHandlers()

        val ll = (view as ScrollView).children.first() as LinearLayout
        dataHandler = DataReceiver {

            val json = JSONObject(it)
            Log.d(TAG, json.getString("msg"))

            json.keys().forEach { key ->
                if (shouldShow(json, key)) {
                    val newTag = key
                    val found = ll.children.find { it.tag == newTag }

                    val newView: TextView
                    if (found == null) {
                        newView = inflater.inflate(R.layout.fragment_consolemode_line, container, false) as TextView
                        ll.addView(newView)
                    } else newView = found as TextView
                    newView.text = key.replace("-text", "") + ": " + json.get(key).toString()
                    newView.tag = newTag
                }
            }
        }
        context?.let { it.registerReceiver(dataHandler, IntentFilter(DECODER_DATA),
            Context.RECEIVER_EXPORTED) }

        return view
    }

    private val skipped = listOf("msg", "SPARE", "crcOk", "FLAGS", "VERSION",
            "gps", "temperature", "decoderType", "origin", "decoderType", "requestId", "emptyFields")    // TODO review all messages

    private fun shouldShow(json: JSONObject, key: String?): Boolean {
        key?.let { k ->
            if (k.endsWith("-text")) return true
            if (json.has("$k-text")) return false
            if (skipped.contains(key)) return false
            return true
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.refreshImage).visibility = INVISIBLE

        activity?.let {
            val app = it.application as MyApp
            val connectedDecoder = app.decoderService.getConnectedDecoder()
            connectedDecoder?.let {cd ->
                app.decoderService.exploreDecoder(cd.uuid)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(dataHandler)
    }

    companion object {

        const val TAG = "ConsoleVostok"

        @JvmStatic
        fun newInstance() = ConsoleModeVostokFragment()
    }
}
