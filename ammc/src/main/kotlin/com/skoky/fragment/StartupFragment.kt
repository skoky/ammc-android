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
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.Decoder
import com.skoky.services.DecoderService.Companion.DECODERS_UPDATE
import com.skoky.services.DecoderService.Companion.DECODER_CONNECT
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED

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

        connectReceiverStateUpdateOrConnect = ConnectionReceiver(this@StartupFragment::visualStateHandlerState)
        connectReceiverDisconnect = ConnectionReceiver(this@StartupFragment::visualStateHandlerDisconnect)
        context?.let {
            it.registerReceiver(connectReceiverStateUpdateOrConnect, IntentFilter(DECODERS_UPDATE),
                Context.RECEIVER_NOT_EXPORTED)
            it.registerReceiver(connectReceiverStateUpdateOrConnect, IntentFilter(DECODER_CONNECT),
                Context.RECEIVER_NOT_EXPORTED)
            it.registerReceiver(connectReceiverDisconnect, IntentFilter(DECODER_DISCONNECTED),
                Context.RECEIVER_NOT_EXPORTED)

        }

        activity?.let {act ->
            val app = act.application as MyApp

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
        }
    }

    private fun visualStateHandler2(foundDecoder: Decoder?) {

        activity?.let {act ->
                    val app = act.application as MyApp

            if (foundDecoder == null) {  // query
                act.findViewById<ProgressBar>(R.id.progressBar2).visibility = VISIBLE
                act.findViewById<Button>(R.id.connectButton).visibility = INVISIBLE
                act.findViewById<TextView>(R.id.moreDecodersButton).visibility = VISIBLE
                act.findViewById<TextView>(R.id.firstDecoderId).text = app.getString(R.string.querying_decoders)
                act.findViewById<TextView>(R.id.firstDecoderId).tag = null
                act.findViewById<Button>(R.id.connectButton).text = app.getString(R.string.connect)

            } else {
                act.findViewById<ProgressBar>(R.id.progressBar2).visibility = INVISIBLE
                act.findViewById<Button>(R.id.connectButton).visibility = VISIBLE
                act.findViewById<Button>(R.id.connectButton).isEnabled = foundDecoder.ipAddress != null

                foundDecoder.let {
                    act.findViewById<TextView>(R.id.firstDecoderId).text = MainActivity.decoderLabel(it)
                    act.findViewById<TextView>(R.id.firstDecoderId).tag = foundDecoder.uuid.toString()
                }

                if (foundDecoder.connection == null) { // not connected
                    act.findViewById<Button>(R.id.connectButton).text = app.getString(R.string.connect)
                    act.findViewById<TextView>(R.id.moreDecodersButton).visibility = VISIBLE
                } else if (foundDecoder.connection != null && foundDecoder.connection!!.isConnected) {  // connected
                    act.findViewById<Button>(R.id.connectButton).text = app.getString(R.string.disconnect)
                    act.findViewById<TextView>(R.id.moreDecodersButton).visibility = INVISIBLE
                }
            }
        }
    }

    private fun visualStateHandlerState(uuidUnsure: String?) {
        activity?.let { act ->
            val app = act.application as MyApp
            app.decoderService.getConnectedDecoder()?.let {
                visualStateHandler2(it)
            }
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


