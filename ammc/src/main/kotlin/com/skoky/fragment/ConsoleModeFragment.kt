package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import org.jetbrains.anko.childrenSequence
import org.json.JSONObject


class ConsoleModeFragment : FragmentCommon() {

    private lateinit var dataHandler: BroadcastReceiver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_consolemode_list, container, false)

        registerConnectionHandlers()

        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        dataHandler = DataReceiver {
            if (!updating) {
                val json = JSONObject(it)
                Log.d(TAG, json.getString("msg"))
                if (json.getString("msg") != "PASSING") {

                    json.keys().forEach { key ->
                        if (shouldShow(json, key)) {
                            val newTag = json.getString("msg").replace("-text", "") + "." + key
                            val found = ll.childrenSequence().find { it.tag == newTag }

                            val newView: TextView
                            if (found == null) {
                                newView = inflater.inflate(R.layout.fragment_consolemode_line, container, false) as TextView
                                ll.addView(newView)
                            } else newView = found as TextView
                            newView.text = key.replace("-text", "") + ": " + json.get(key).toString()
                            newView.tag = newTag
                        }
                    }
                }
            }
        }
        context?.let { it.registerReceiver(dataHandler, IntentFilter(DECODER_DATA)) }

        return view
    }

    private val skipped = listOf("msg", "SPARE", "crcOk", "FLAGS", "VERSION",
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
        activity?.let { a ->
            val app = a.application as MyApp
            val connectedDecoder = app.decoderService.getConnectedDecoder()

            connectedDecoder?.let { decoder ->
                app.decoderService.exploreDecoder(decoder.uuid)
                val iv = view.findViewById<ImageView>(R.id.refreshImage)
                iv.setOnClickListener {
                    doRefresh()
                }
            }
        }
    }

    private var updating = false
    private fun doRefresh() {
        updating = true
        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        ll.childrenSequence().iterator().withIndex().forEach { i ->
            if (i.index > 0) (i.value as? TextView)?.text = ""
        }
        updating = false

        activity?.let {a ->
            val app = a.application as MyApp
            val connectedDecoder = app.decoderService.getConnectedDecoder()
            if (connectedDecoder==null) {
                    (a as MainActivity).openStartupFragment()
            } else
                app.decoderService.exploreDecoder(connectedDecoder.uuid)

        }
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(dataHandler)
    }

    companion object {

        const val TAG = "ConsoleModeFragment"

        @JvmStatic
        fun newInstance() = ConsoleModeFragment()
    }

}
