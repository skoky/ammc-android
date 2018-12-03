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
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.Decoder
import com.skoky.services.DecoderService.Companion.DECODERS_UPDATE
import com.skoky.services.DecoderService.Companion.DECODER_CONNECT
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import kotlinx.android.synthetic.main.startup_content.*

class StartupFragment : FragmentCommon() {

    //    private var decoderFoundText: TextView? = null
    private lateinit var connectReceiverDisconnect: BroadcastReceiver
    private lateinit var connectReceiverStateUpdateOrConnect: BroadcastReceiver

    // FIXME connected receiver show up very late after connect button pressed...
    class ConnectionReceiver(val stateHandler: (String?) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val uuid = intent?.getStringExtra("uuid")
            stateHandler(uuid)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = activity!!.application as MyApp

        connectReceiverStateUpdateOrConnect = ConnectionReceiver(this@StartupFragment::visualStateHandlerState)
        connectReceiverDisconnect = ConnectionReceiver(this@StartupFragment::visualStateHandlerDisconnect)
        context!!.registerReceiver(connectReceiverStateUpdateOrConnect, IntentFilter(DECODERS_UPDATE))
        context!!.registerReceiver(connectReceiverStateUpdateOrConnect, IntentFilter(DECODER_CONNECT))
        context!!.registerReceiver(connectReceiverDisconnect, IntentFilter(DECODER_DISCONNECTED))

        Log.i(TAG, app.decoderService.getDecoders().toString())
        if (app.decoderService.getDecoders().isNullOrEmpty()) {
            val connectedDecoder = app.decoderService.getConnectedDecoder()
            if (connectedDecoder != null)
                visualStateHandler2(connectedDecoder)
            else {
                app.decoderService.getBestFreeDecoder()?.let {
                    visualStateHandler2(it)
                }
            }
        }

        registerConnectionHandlers()
    }

    private fun visualStateHandler2(foundDecoder: Decoder?) {
        val app = activity!!.application as MyApp

        if (foundDecoder == null) {  // query
            progressBar2.visibility = VISIBLE
            connectButton.visibility = INVISIBLE
            moreDecodersButton.visibility = VISIBLE
            firstDecoderId.text = app.getString(R.string.querying_decoders)
            firstDecoderId.tag = null
            connectButton.text = app.getString(R.string.connect)

        } else {
            progressBar2.visibility = INVISIBLE
            connectButton.visibility = VISIBLE
            connectButton.isEnabled = foundDecoder.ipAddress != null

            foundDecoder.let {
                firstDecoderId.text = MainActivity.decoderLabel(it)
                firstDecoderId.tag = foundDecoder.uuid.toString()
            }

            if (foundDecoder.connection == null) { // not connected
                connectButton.text = app.getString(R.string.connect)
                moreDecodersButton.visibility = VISIBLE
            } else if (foundDecoder.connection != null && foundDecoder.connection!!.isConnected) {  // connected
                connectButton.text = app.getString(R.string.disconnect)
                moreDecodersButton.visibility = INVISIBLE
            }
        }
    }

    private fun visualStateHandlerState(uuidUnsure: String?) {
        val app = activity!!.application as MyApp
        app.decoderService.getConnectedDecoder()?.let {
            visualStateHandler2(it)
        }
    }
    private fun visualStateHandlerDisconnect(uuidUnsure: String?) {
        visualStateHandler2(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.startup_content, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        context?.let {
            it.unregisterReceiver(connectReceiverStateUpdateOrConnect)
            it.unregisterReceiver(connectReceiverDisconnect)
        }
    }

    companion object {
        private const val TAG = "StartupFragment"
    }
}


