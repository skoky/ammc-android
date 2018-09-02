package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.skoky.MainActivity
import com.skoky.MyApp


import com.skoky.R
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import com.skoky.services.DecoderService.Companion.DECODER_CONNECT
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import eu.plib.Parser
import eu.plib.ParserS
import kotlinx.android.synthetic.main.startup_content.*
import org.json.JSONObject
import java.util.*

class StartupFragment : Fragment() {

    //    private var decoderFoundText: TextView? = null
    private lateinit var connectReceiver: BroadcastReceiver
    private lateinit var dataReceiver: BroadcastReceiver

    class ConnectionReceiver(val app: MyApp, private val bar: ProgressBar, val button: Button,
                             private val decoderText: TextView) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val decoders = app.decoderService?.getDecoders()
            val uuid = UUID.fromString(intent?.getStringExtra("uuid")!!)

            val foundDecoder = decoders!!.find { it.uuid == uuid }

            if (foundDecoder == null) {  // query
                bar.visibility = VISIBLE
                button.visibility = INVISIBLE
                decoderText.text = app.getString(R.string.querying_decoders)
                button.text = app.getString(R.string.connect)

            } else {

                if (foundDecoder!!.connection == null) { // not connected
                    bar.visibility = INVISIBLE
                    button.visibility = VISIBLE
                    button.text = app.getString(R.string.connect)
                    foundDecoder?.let {
                        decoderText.text = MainActivity.decoderLabel(it)
                        decoderText.tag = foundDecoder.uuid.toString()
                    }
                } else if (foundDecoder!!.connection != null && foundDecoder!!.connection!!.isBound) {  // connected
                    button.text = app.getString(R.string.disconnect)
                }
            }
        }
    }


    class DataReceiver(val app: MyApp, private val bar: ProgressBar, val button: Button,
                       private val decoderText: TextView) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val decoders = app.decoderService!!.getDecoders()
            val lastMessageFromDecoder = intent!!.getStringExtra("Data")

            if (lastMessageFromDecoder==null) Log.w(TAG,"No data inside intent")

            lastMessageFromDecoder?.let {msg ->
                Log.d(TAG, "Last msg: $msg")
                val json = JSONObject(msg)

                json.get("decoderId")?.let { dId ->     // update decoder data
                    decoders.find { it.decoderId == dId }?.let { d ->
                        decoderText.text = MainActivity.decoderLabel(d)
                        decoderText.tag = d.uuid.toString()
                        bar.visibility = INVISIBLE
                        button.visibility = VISIBLE
                    }
                }

                if (json.has("recordType")) when (json.get("recordType")) {
                    "NetworkSettings" -> ""// TBD
                }
            }


        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = activity!!.application as MyApp

        connectReceiver = ConnectionReceiver(app, progressBar2, connectButton, firstDecoderId)
        context!!.registerReceiver(connectReceiver, IntentFilter(DECODER_CONNECT))
        context!!.registerReceiver(connectReceiver, IntentFilter(DECODER_DISCONNECTED))

        dataReceiver = DataReceiver(app, progressBar2, connectButton, firstDecoderId)
        context!!.registerReceiver(dataReceiver, IntentFilter(DECODER_DATA))

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.startup_content, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(connectReceiver)
    }

    companion object {
        private const val TAG = "StartupFragment"
    }
}


