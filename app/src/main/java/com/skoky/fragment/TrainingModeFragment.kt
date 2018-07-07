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
import com.skoky.R
import com.skoky.fragment.content.Lap
import com.skoky.services.PassingBroadcastReceiver
import org.json.JSONObject


class TrainingModeFragment : Fragment() {

    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var receiver: PassingBroadcastReceiver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_trainingmode_list, container, false)

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
                    (adapter as TrainingModeRecyclerViewAdapter).addRecord(transponder, time)
                    adapter.notifyDataSetChanged()
                }
                context!!.registerReceiver(receiver, IntentFilter("com.skoky.decoder.broadcast.passing"))

//                doAsync {
//                    var i = 0
//                    while(i<10) {
//                        uiThread {
//                            (adapter as TrainingModeRecyclerViewAdapter).addRecord(TrainingModeModel.Lap(i,2L,3, 4f))
//                            adapter.notifyDataSetChanged()
//                        }
//                        Thread.sleep(2000)
//                        i++
//                    }
//                }
            }
        }


        return view
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
