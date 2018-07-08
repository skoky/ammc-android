package com.skoky.fragment

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.skoky.R
import com.skoky.fragment.content.Lap
import com.skoky.fragment.content.TrainingModeModel
import com.skoky.services.PassingBroadcastReceiver
import kotlinx.android.synthetic.main.fragment_trainingmode_list.*
import org.json.JSONObject


class TrainingModeFragment : Fragment() {

    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var receiver: PassingBroadcastReceiver

    private var startStopButtonM: Button? = null

    private lateinit var tmm: TrainingModeModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_trainingmode_list, container, false)
        val transponders = mutableListOf<String>()

        val viewContent = view.findViewById<RecyclerView>(R.id.training_content)
        // Set the adapter
        if (viewContent is RecyclerView) {
            with(viewContent) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = TrainingModeRecyclerViewAdapter(mutableListOf(), listener)

                receiver = PassingBroadcastReceiver()
                receiver.setHandler { data ->
                    val json = JSONObject(data)
                    Log.i(TAG, "Received passing $data")
                    val transponder = json.get("transponder") as Int
                    val time = (json.get("RTC_Time") as String).toLong()

                    if (running) {
                        (adapter as TrainingModeRecyclerViewAdapter).addRecord(transponder, time)
                        adapter.notifyDataSetChanged()
                    }
                    tmm = (adapter as TrainingModeRecyclerViewAdapter).tmm

                    if (!transponders.contains(transponder.toString())) {
                        transponders.add(transponder.toString())
                        view.findViewById<Spinner>(R.id.decoderIdSpinner).adapter = ArrayAdapter(view.context,
                                android.R.layout.simple_list_item_1, transponders)
                    }
                }
                context!!.registerReceiver(receiver, IntentFilter("com.skoky.decoder.broadcast.passing"))
            }
        }
        startStopButtonM = view.findViewById<Button>(R.id.startStopButton)
        startStopButtonM!!.setOnClickListener { doStartStop()}

        return view
    }

    private var running = false
    fun doStartStop() {
        running = !running
        if (running) {
            startStopButtonM?.text = getText(R.string.stop)
        } else {
            startStopButtonM?.text = getText(R.string.start)
        }

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
        fun onListFragmentInteraction(item: Lap?)
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "TrainingModeFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                TrainingModeFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
