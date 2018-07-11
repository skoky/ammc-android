package com.skoky.fragment

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.DecoderBroadcastReceiver
import com.skoky.services.DecoderService.Companion.DECODER_REFRESH
import kotlinx.android.synthetic.main.startup_content.*
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.find

class StartupFragment : Fragment() {

    private val TAG = "StartupFragment"
    private var columnCount = 1

    private var lastMessageFromDecoder: ByteArray? = null
    private var decoderFound: TextView? = null
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
                    val name = if (d.decoderType != null) d.decoderType else d.id
                    if (!d.ipAddress.isNullOrEmpty()) {
                        decoderFound!!.text = "$name / ${d.ipAddress}"
                    } else {
                        decoderFound!!.text = "$name"
                    }
                    progressBar2.visibility = INVISIBLE
                    connectButton.visibility = VISIBLE
//                    moreDecodersButton.visibility = INVISIBLE

                    if (d.connection != null) {
                        connectButton.text = getString(R.string.disconnect)
                    } else {
                        connectButton.text = getString(R.string.connect)
                    }

                } else {
                    decoderFound!!.text = getString(R.string.querying_decoders)
                    progressBar2.visibility = VISIBLE
                    connectButton.visibility = INVISIBLE
//                    moreDecodersButton.visibility = INVISIBLE
                }

                if (it.getDecoders().size > 1) {
//                    moreDecodersButton.visibility = VISIBLE
                }

            }
        }
        context!!.registerReceiver(receiver, IntentFilter(DECODER_REFRESH))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.startup_content, container, false)
        decoderFound = view.find<TextView>(R.id.firstDecoderId)
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
