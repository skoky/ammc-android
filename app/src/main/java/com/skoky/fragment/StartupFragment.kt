package com.skoky.fragment

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
import com.skoky.fragment.content.StartupContent.DummyItem
import com.skoky.services.DecodeBroadcastReceiver
import kotlinx.android.synthetic.main.main_content.*
import org.jetbrains.anko.find

class StartupFragment : Fragment() {

    private val TAG = "StartupFragment"
    private var columnCount = 1
    private var listener: OnListFragmentInteractionListener? = null

    private var lastMessageFromDecoder: ByteArray? = null
    private var decoderFound: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = activity!!.application as MyApp

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        val receiver = DecodeBroadcastReceiver()
        receiver.setHandler { data ->
            app.decoderService?.let {


                lastMessageFromDecoder = data

                if (it.getDecoders().isNotEmpty()) {
                    val d = it.getDecoders().first()
                    val name = if (d.decoderType != null) d.decoderType else d.id
                    if (!d.ipAddress.isNullOrEmpty()) {
                        decoderFound!!.text = "${name} / ${d.ipAddress}"
                    } else {
                        decoderFound!!.text = "${name}"
                    }
                    progressBar2.visibility = INVISIBLE
                    connectButton.visibility = VISIBLE
                    moreDecodersButton.visibility = INVISIBLE

                    if (d.connection != null) {
                        connectButton.text = getString(R.string.disconnect)
                    } else {
                        connectButton.text = getString(R.string.connect)
                    }

                } else {
                    decoderFound!!.text = getString(R.string.querying_decoders)
                    progressBar2.visibility = VISIBLE
                    connectButton.visibility = INVISIBLE
                    moreDecodersButton.visibility = INVISIBLE
                }

                if (it.getDecoders().size > 1) {
                    moreDecodersButton.visibility = VISIBLE
                }

            }
        }
        context!!.registerReceiver(receiver, IntentFilter("com.skoky.decoder.broadcast"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.main_content, container, false)
        decoderFound = view.find<TextView>(R.id.firstDecoderId)
        return view
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: DummyItem?)
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
