package com.skoky.fragment

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.skoky.R
import com.skoky.Tools
import com.skoky.fragment.content.RacingLap
import com.skoky.fragment.content.RacingModeModel
import com.skoky.services.DecoderService.Companion.DECODER_DISCONNECTED
import com.skoky.services.DecoderService.Companion.DECODER_PASSING
import kotlinx.android.synthetic.main.fragment_racingmode.view.*
import kotlinx.android.synthetic.main.fragment_racingmode_list.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.util.concurrent.Future


class RacingModeFragment : Fragment() {

    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var receiver: BroadcastReceiver

    private var startStopButtonM: Button? = null

    private var tmm: RacingModeModel = RacingModeModel()    // a dummy model with no transponder
    private val transponders = mutableListOf<String>()

    private lateinit var timingContentView: RecyclerView

    private lateinit var clockViewX: TextView

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    class PassingDataReceiver(val handler: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler(intent!!.getStringExtra("Passing")!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_racingmode_list, container, false)

        clockViewX = view.clockView
        timingContentView = view.training_content
        // Set the adapter

        with(timingContentView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            adapter = RacingModeRecyclerViewAdapter(mutableListOf(), listener)

            receiver = PassingDataReceiver { data ->
                val json = JSONObject(data)
                Log.i(TAG, "Received passing $data")
                val transponder = json.get("transponder") as Int
                val time = (json.get("RTC_Time") as String).toLong()

                if (running) {
                    (adapter as RacingModeRecyclerViewAdapter).addRecord(transponder, time)
                    adapter.notifyDataSetChanged()
                }
                tmm = (adapter as RacingModeRecyclerViewAdapter).tmm

                if (!transponders.contains(transponder.toString())) {
                    transponders.add(transponder.toString())
                }
            }
            context!!.registerReceiver(receiver, IntentFilter(DECODER_PASSING))

        }
        startStopButtonM = view.startStopButton
        startStopButtonM!!.setOnClickListener { doStartStopDialog() }

        val disconnectReceiver = ConnectionReceiver {
            Log.i(TAG,"Disconnected")
            //AlertDialog.Builder(context).setMessage(getString(R.string.decoder_not_connected)).setCancelable(true).create().show()
        }
        context!!.registerReceiver(disconnectReceiver, IntentFilter(DECODER_DISCONNECTED))

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.findViewById<View>(R.id.miHome).visibility = VISIBLE     // FIXME does not work :(
    }

    var running = false
    private fun doStartStopDialog() {
        doStartStop()
    }

    private fun doStartStop() {

        if (running) {
            running = false
            clock.cancel(true)      // TODO calculate exact training timeUs
            startStopButtonM?.text = getText(R.string.start)
        } else {    // not running

            if (timingContentView.adapter.itemCount == 1) {     // just a label, nothing to clear
                doStart()
            } else {
                AlertDialog.Builder(context).setTitle("Clear results and start new training?")
                        .setPositiveButton("Yes") { dialog, _ ->
                            dialog.cancel()
                            doStart()
                        }
                        .setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                        .create().show()
            }
        }

    }

    private lateinit var clock: Future<Unit>

    private var racingStartTime: Long? = null

    private fun doStart() {
        (timingContentView.adapter as RacingModeRecyclerViewAdapter).clearResults()
        running = true
        startStopButtonM?.text = getText(R.string.stop)
        racingStartTime = System.currentTimeMillis()

        clock = doAsync {
            while (true) {
                val timeMs = System.currentTimeMillis() - racingStartTime!!
                val str = Tools.millisToTimeWithMillis(timeMs)
                uiThread {
                    clockViewX.text = str
                }
                Thread.sleep(30)
            }
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
        fun onListFragmentInteraction(item: RacingLap?)
    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "RacingModeFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                RacingModeFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
