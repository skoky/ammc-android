package com.skoky.fragment

import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.DecoderBroadcastReceiver
import com.skoky.services.DecoderService.Companion.DECODER_REFRESH
import kotlinx.android.synthetic.main.startup_content.*
import org.jetbrains.anko.find

class StartupFragment : Fragment() {

    private val TAG = "StartupFragment"
    private var columnCount = 1

    private var lastMessageFromDecoder: ByteArray? = null
    private var decoderFoundText: TextView? = null
    private lateinit var receiver : DecoderBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = activity!!.application as MyApp

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        receiver = DecoderBroadcastReceiver()
        receiver.setHandler { data ->
            app.decoderService?.let {

                lastMessageFromDecoder = data

                if (it.getDecoders().isNotEmpty()) {
                    val d = it.getDecoders().first()
                    Log.i(TAG,"Decoder found $d")
                    decoderFoundText?.let { it.text = MainActivity.decoderLabel(d) }

                    progressBar2.visibility = INVISIBLE
                    connectButton.visibility = VISIBLE

                    if (d.connection != null) {
                        connectButton.text = getString(R.string.disconnect)
                    } else {
                        connectButton.text = getString(R.string.connect)
                    }

                } else {
                    decoderFoundText?.let { it.text = getString(R.string.querying_decoders) }
                    progressBar2.visibility = VISIBLE
                    connectButton.visibility = INVISIBLE
                }
            }
        }
        context!!.registerReceiver(receiver, IntentFilter(DECODER_REFRESH))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.startup_content, container, false)
        decoderFoundText = view.find<TextView>(R.id.firstDecoderId)
        return view
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(receiver)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
                StartupFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }
}
