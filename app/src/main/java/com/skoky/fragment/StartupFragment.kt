package com.skoky.fragment

import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.StartupContent.DummyItem
import com.skoky.services.DecodeBroadcastReceiver
import com.skoky.services.DecoderService
import eu.plib.Parser
import org.jetbrains.anko.find
import org.json.JSONObject

class StartupFragment : Fragment() {

    private val TAG = "StartupFragment"
    private var columnCount = 1
    private var listener: OnListFragmentInteractionListener? = null

    var lastMessageFromDecoder: ByteArray? = null
    var ds: TextView? = null
    var df: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = activity!!.application as MyApp

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        val receiver = DecodeBroadcastReceiver()
        receiver.setHandler { data ->
            app.decoderService?.let {

                val decodersSize = it.getDecoders().size
                lastMessageFromDecoder = data
                ds?.let { ds -> ds.text = "Decoder: $decodersSize" }

                val d = it.getDecoders().first()
                df?.let{
                    it.text = "${d.id} / ${d.ipAddress}"
                }

            }

        }
        context!!.registerReceiver(receiver, IntentFilter("com.skoky.decoder.broadcast"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.main_content, container, false)
        ds = view.findViewById<TextView>(R.id.decodersFound)
        df = view.find<TextView>(R.id.firstDecoderId)
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
