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

import com.skoky.R
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import com.skoky.services.DecoderService.Companion.DECODER_CONNECT
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import kotlinx.android.synthetic.main.startup_content.*
import org.jetbrains.anko.find

class StartupFragment : Fragment() {

    //    private var decoderFoundText: TextView? = null
    private lateinit var connectReceiver: BroadcastReceiver
    private lateinit var disconnectReceiver: BroadcastReceiver
    private lateinit var dataReceiver: BroadcastReceiver

    class ConnectionReceiver(private val bar: ProgressBar, val button: Button,
                             val text: String, val decoderText: TextView, val decoderLabel: String) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            bar.visibility = INVISIBLE
            button.visibility = VISIBLE
            button.text = text
            decoderText.text = decoderLabel
        }
    }


    class DataReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val lastMessageFromDecoder = intent!!.getByteArrayExtra("Data")
            Log.d(TAG, "Last msg: $lastMessageFromDecoder")

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectReceiver = ConnectionReceiver(progressBar2, connectButton, getString(R.string.disconnect), firstDecoderId, "xxx")
        disconnectReceiver = ConnectionReceiver(progressBar2, connectButton, getString(R.string.connect), firstDecoderId, getString(R.string.querying_decoders))
        context!!.registerReceiver(connectReceiver, IntentFilter(DECODER_CONNECT))
        context!!.registerReceiver(disconnectReceiver, IntentFilter(DECODER_DISCONNECTED))


        dataReceiver = DataReceiver()
        context!!.registerReceiver(dataReceiver, IntentFilter(DECODER_DATA))

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.startup_content, container, false)
//        decoderFoundText = view.find<TextView>(R.id.firstDecoderId)
        return view
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(connectReceiver)
    }

    companion object {
        private const val TAG = "StartupFragment"
    }
}


