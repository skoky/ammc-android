package com.skoky.fragment

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import com.skoky.services.DecoderService.Companion.DECODER_DATA
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import kotlinx.android.synthetic.main.fragment_consolemode_list.*
import org.json.JSONObject
import java.util.*


class ConsoleModeFragment : Fragment() {

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var receiver: BroadcastReceiver

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

        //adapter = ConsoleModeRecyclerViewAdapter(mutableListOf(), listener)

        val disconnectReceiver = ConnectionReceiver {
            Log.i(TAG, "Disconnected")
            AlertDialog.Builder(context).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
        }
        context!!.registerReceiver(disconnectReceiver, IntentFilter(DECODER_DISCONNECTED))

        val dataHandler = DataReceiver {
            val json = JSONObject(it)
            if (json.has("decoderId")) decoderIdText.text = getString(R.string.decoderId) + json.getString("decoderId")
        }
        context!!.registerReceiver(dataHandler, IntentFilter(DECODER_DATA))

        return view
    }

    var decoderId : UUID? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.findViewById<View>(R.id.miHome).visibility = VISIBLE     // FIXME does not work :(

//        decoderId = (firstDecoderId.tag as? String)?.let { UUID.fromString(it) }

        // TODO init screen from data already have
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(receiver)
    }


    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: ConsoleModel?)
    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "ConsoleModeFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                ConsoleModeFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
