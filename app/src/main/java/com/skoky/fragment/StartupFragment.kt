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
import com.skoky.services.DecoderService.Companion.DECODER_CONNECT
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import kotlinx.android.synthetic.main.startup_content.*
import org.json.JSONObject
import java.util.*

class StartupFragment : Fragment() {

    //    private var decoderFoundText: TextView? = null
    private lateinit var connectReceiver: BroadcastReceiver
    private lateinit var dataReceiver: BroadcastReceiver


    // FIXME connected receiver show up very late after connect button pressed...
    class ConnectionReceiver(val stateHandler: (UUID) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val uuid = UUID.fromString(intent?.getStringExtra("uuid")!!)
            stateHandler(uuid)
        }
    }


    class DataReceiver(val app: MyApp, private val bar: ProgressBar, val button: Button,
                       private val decoderText: TextView) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val decoders = app.decoderService!!.getDecoders()
            val lastMessageFromDecoder = intent!!.getStringExtra("Data")

            if (lastMessageFromDecoder == null) Log.w(TAG, "No data inside intent")

            lastMessageFromDecoder?.let { msg ->
                Log.d(TAG, "Last msg: $msg")
                val json = JSONObject(msg)

                json.get("decoderId")?.let { dId ->
                    // update decoder data
                    decoders.find { it.decoderId == dId }?.let { d ->
                        decoderText.text = MainActivity.decoderLabel(d)
                        decoderText.tag = d.uuid.toString()
                        bar.visibility = INVISIBLE
                        button.visibility = VISIBLE
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = activity!!.application as MyApp

        connectReceiver = ConnectionReceiver(this@StartupFragment::visualStateHandler)
        context!!.registerReceiver(connectReceiver, IntentFilter(DECODER_CONNECT))
        context!!.registerReceiver(connectReceiver, IntentFilter(DECODER_DISCONNECTED))

        dataReceiver = DataReceiver(app, progressBar2, connectButton, firstDecoderId)
        context!!.registerReceiver(dataReceiver, IntentFilter(DECODER_DATA))

        app.decoderService?.let {
            if (it.getDecoders().isNotEmpty())
                visualStateHandler(it.getDecoders().first().uuid)
        }

    }

    private fun visualStateHandler(uuid: UUID) {

        val app = activity!!.application as MyApp

        val foundDecoder = app.decoderService!!.getDecoders().find { it.uuid == uuid }

        if (foundDecoder == null) {  // query
            progressBar2.visibility = VISIBLE
            connectButton.visibility = INVISIBLE
            firstDecoderId.text = app.getString(R.string.querying_decoders)
            firstDecoderId.tag = null
            connectButton.text = app.getString(R.string.connect)

        } else {

            if (foundDecoder!!.connection == null) { // not connected
                progressBar2.visibility = INVISIBLE
                connectButton.visibility = VISIBLE
                connectButton.text = app.getString(R.string.connect)
                foundDecoder?.let {
                    firstDecoderId.text = MainActivity.decoderLabel(it)
                    firstDecoderId.tag = foundDecoder.uuid.toString()
                }
            } else if (foundDecoder!!.connection != null && foundDecoder!!.connection!!.isBound) {  // connected
                connectButton.text = app.getString(R.string.disconnect)
            }
        }
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


