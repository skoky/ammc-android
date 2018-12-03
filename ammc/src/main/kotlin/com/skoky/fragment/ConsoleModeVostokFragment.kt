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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import kotlinx.android.synthetic.main.fragment_consolemode_list.*
import org.jetbrains.anko.childrenSequence
import org.json.JSONObject


class ConsoleModeVostokFragment : FragmentCommon() {

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var dataHandler: BroadcastReceiver

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    class DataReceiver(val handler: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler(intent!!.getStringExtra("Data")!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_consolemode_list, container, false)

        registerConnectionHandlers()

        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        dataHandler = DataReceiver {

            val json = JSONObject(it)
            Log.d(TAG, json.getString("recordType"))
//                if (json.getString("recordType") != "Passing") {

            json.keys().forEach { key ->
                if (shouldShow(json, key)) {
                    val newTag = key
                    var found = ll.childrenSequence().find { it.tag == newTag }

                    var newView: TextView
                    if (found == null) {
                        newView = inflater.inflate(R.layout.fragment_consolemode_line, container, false) as TextView
                        ll.addView(newView)
                    } else newView = found as TextView
                    newView.text = key.replace("-text", "") + ": " + json.get(key).toString()
                    newView.tag = newTag
                }
            }
            //     }

        }
        context!!.registerReceiver(dataHandler, IntentFilter(DECODER_DATA))

        return view
    }

    private val skipped = listOf("recordType", "SPARE", "crcOk", "FLAGS", "VERSION",
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
        activity!!.findViewById<View>(R.id.miHome).visibility = VISIBLE     // FIXME does not work :(
        val app = activity!!.application as MyApp
        val connectedDecoder = app.decoderService.getConnectedDecoder()

        app.decoderService.exploreDecoder(connectedDecoder?.uuid!!)
        refreshImage.visibility = INVISIBLE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: ConsoleModel?)
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(dataHandler)
    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "ConsoleVostok"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                ConsoleModeVostokFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }
}
