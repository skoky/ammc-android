package com.skoky.fragment

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.Tools
import com.skoky.Wrapped.sleep
import com.skoky.fragment.content.TrainingLap
import com.skoky.fragment.content.TrainingModeModel
import com.skoky.services.DecoderService.Companion.DECODER_PASSING
import kotlinx.android.synthetic.main.fragment_trainingmode_list.*
import kotlinx.android.synthetic.main.fragment_trainingmode_list.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.util.concurrent.Future


class TrainingModeFragment : FragmentCommon() {

    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var receiver: BroadcastReceiver

    lateinit var startStopButtonM: Button

    private var tmm: TrainingModeModel = TrainingModeModel()    // a dummy model with no recentTransponders

    private lateinit var timingContentView: RecyclerView

    private lateinit var clockViewX: TextView

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    class PassingDataReceiver(val handler: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                handler(it.getStringExtra("Passing"))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_trainingmode_list, container, false)

        clockViewX = view.clockView
        timingContentView = view.training_content
        // Set the adapter

        with(timingContentView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            adapter = TrainingModeRecyclerViewAdapter(mutableListOf(), listener)

            receiver = PassingDataReceiver { data ->
                val json = JSONObject(data)
                Log.i(TAG, "Received passing $data")
                val transponder = FragmentCommon().getTransponderFromPassingJson(activity!!.application, json)
                val timeUs = FragmentCommon().getTimeFromPassingJson(json)

                if (trainingRunning) {
                    (adapter as TrainingModeRecyclerViewAdapter).addRecord(transponder, timeUs)
                    adapter.notifyDataSetChanged()
                }
                tmm = (adapter as TrainingModeRecyclerViewAdapter).tmm
            }
            context?.let {
                it.registerReceiver(receiver, IntentFilter(DECODER_PASSING))
            }

        }
        view.startStopButton.setOnClickListener { doStartStopDialog() }
        startStopButtonM = view.startStopButton
        registerConnectionHandlers()

        return view
    }

    fun openTransponderDialog(startRace: Boolean) {

        activity?.let { act ->
            val app = act.application as MyApp
            val trs = app.recentTransponders.toTypedArray()

            val b = android.support.v7.app.AlertDialog.Builder(act)
                    .setTitle(getString(R.string.select_label))
            if (trs.isEmpty()) {
                b.setMessage(getString(R.string.no_transponder))
            } else {
                b.setSingleChoiceItems(trs, 0) { dialog, i ->
                    Log.w(TAG, "Selected $i")
                    setSelectedTransponder(trs[i])
                    decoderIdSelector.text = trs[i]
                    if (startRace) doStartStop()
                    dialog.cancel()
                }
            }
            b.create().show()
        }
    }

    private fun setSelectedTransponder(transponder: String) {
        tmm.setSelectedTransponder(transponder)
    }

    var trainingRunning = false
    var preStartDelayRunning = false
    private fun doStartStopDialog() {

        if (tmm.getSelectedTransponder() == null) {
            openTransponderDialog(true)
            return
        }
        doStartStop()
    }

    private fun doStartStop() {

        if (trainingRunning || preStartDelayRunning) {
            doStop()
        } else {    // not trainingRunning

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

    private var clock: Future<Unit>? = null

    private fun doStop() {
        if (preStartDelayRunning) {
            clockViewX.text = ""
        }
        trainingRunning = false
        preStartDelayRunning = false
        clock?.cancel(true)      // TODO calculate exact training timeUs
        startStopButtonM.text = getText(R.string.start)
    }

    private fun doStart() {

        val ma = (activity as MainActivity)
        val startupDelay = ma.getStartupDelayFlag()

        if (startupDelay) {
            doStartDelay(ma.getStartupDelayValueFlag())
        } else {
            doStartNow()
        }
    }

    private fun doStartDelay(delaySecs: Int) {
        preStartDelayRunning = true
        (timingContentView.adapter as TrainingModeRecyclerViewAdapter).clearResults()
        startStopButtonM.text = getText(R.string.stop)
        var isInterrupted = false
        val delayStartTime = System.currentTimeMillis()

        clock = doAsync {
            while (System.currentTimeMillis() - delayStartTime < delaySecs * 1000 && preStartDelayRunning && !isInterrupted) {
                val diffSecs = (System.currentTimeMillis() - delayStartTime) / 1000
                val time = delaySecs - diffSecs
                val str = "Start in ${time}s"
                uiThread {
                    clockViewX.text = str
                }
                isInterrupted = sleep(30)
            }

            if (!isInterrupted) {
                uiThread {
                    doStartNow()
                }
            }
        }
    }

    private fun doStartNow() {
        (timingContentView.adapter as TrainingModeRecyclerViewAdapter).clearResults()
        preStartDelayRunning = false
        trainingRunning = true
        startStopButtonM.text = getText(R.string.stop)
        val trainingStartTime = System.currentTimeMillis()
        var isInterrupted = false

        clock = doAsync {
            while (trainingRunning && !isInterrupted) {
                val timeMs = System.currentTimeMillis() - trainingStartTime
                val str = Tools.millisToTimeWithMillis(timeMs)
                uiThread {
                    clockViewX.text = str
                }
                isInterrupted = sleep(30)
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
        clock?.cancel(true)
    }

    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: TrainingLap?)
    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
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
