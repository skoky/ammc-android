package com.skoky.fragment

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.Tone.preStartTone
import com.skoky.Tone.startTone
import com.skoky.Tone.stopTone
import com.skoky.Tools
import com.skoky.Wrapped.sleep
import com.skoky.fragment.content.RacingModeModel
import com.skoky.services.DecoderService.Companion.DECODER_PASSING
import kotlinx.android.synthetic.main.fragment_racingmode_list.view.*
import org.jetbrains.anko.childrenSequence
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.util.concurrent.Future


class RacingModeFragment : FragmentCommon() {

    private lateinit var receiver: BroadcastReceiver

    private lateinit var startStopButtonM: Button

    private var tmm: RacingModeModel = RacingModeModel()    // a dummy model with no recentTransponders
    private val transponders = mutableListOf<String>()

    private lateinit var timingContentView: RecyclerView

    private lateinit var clockViewX: TextView

    private lateinit var mAdapter: RacingModeRecyclerViewAdapter

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    class PassingDataReceiver(val handler: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                handler(it.getStringExtra("Passing") ?: "")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_racingmode_list, container, false)
        clockViewX = view.clockView
        timingContentView = view.training_content
        // Set the adapter

        with(timingContentView) {
            layoutManager = LinearLayoutManager(context)

            adapter = RacingModeRecyclerViewAdapter(mutableListOf()) { handleDriverEditDialog(it) }
            mAdapter = adapter as RacingModeRecyclerViewAdapter
            receiver = PassingDataReceiver { data ->
                val json = JSONObject(data)
                Log.i(TAG, "Received passing $data")

                val transponder = FragmentCommon().getTransponderFromPassingJson(json)
                val time = FragmentCommon().getTimeFromPassingJson(json)

                if (raceRunning) {
                    (adapter as RacingModeRecyclerViewAdapter).addRecord(transponder, time)
                    (adapter as RacingModeRecyclerViewAdapter).notifyDataSetChanged()
                    doAsync {
                        activity?.let {
                            val myApp = it.application as MyApp
                            (adapter as RacingModeRecyclerViewAdapter).updateDriverName(myApp, transponder)
                        }
                    }
                }
                tmm = (adapter as RacingModeRecyclerViewAdapter).tmm

                if (!transponders.contains(transponder)) {
                    transponders.add(transponder)
                }
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

    private fun handleDriverEditDialog(view: View) {
        val views = view.childrenSequence().iterator()
        views.next()
        val transOrDriver = (views.next() as TextView).text

        val dialog = AlertDialog.Builder(activity).setCancelable(true)
                .setView(R.layout.update_deriver_dialog).show()
        val transET = dialog.findViewById<EditText>(R.id.transponder_edit_text)
        transOrDriver?.let { transET.setText(mAdapter.getTransponder(transOrDriver.toString())) }
        val driverET = dialog.findViewById<EditText>(R.id.driver_name_edit_text)
        dialog.findViewById<Button>(R.id.cancel_button).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.save_button).setOnClickListener {
            activity?.let { act ->
                val app = act.application as MyApp
                mAdapter.saveDriverName(app, transET.text.toString(), driverET.text.toString())
            }
            dialog.dismiss()
        }

        driverET?.let {
            it.setText(mAdapter.getDriverForTransponder(transOrDriver))
            it.requestFocus()
        }
    }

    private var raceRunning = false
    private var preStartDelayRunning = false
    private fun doStartStopDialog() {
        doStartStop()
    }

    private fun doStop() {
        val ma = (activity as MainActivity)
        if (ma.getStartStopSoundFlag()) stopTone()

        if (preStartDelayRunning) {
            clockViewX.text = ""
        }
        raceRunning = false
        preStartDelayRunning = false
        clock?.cancel(true)      // TODO calculate exact training timeUs
        startStopButtonM?.text = getText(R.string.start)
    }

    private fun doStartStop() {

        if (raceRunning || preStartDelayRunning) {
            doStop()
        } else {    // not raceRunning

            if (timingContentView.adapter?.itemCount == 1) {     // just a label, nothing to clear
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
        (timingContentView.adapter as RacingModeRecyclerViewAdapter).clearResults()
        startStopButtonM?.text = getText(R.string.stop)
        val ma = (activity as MainActivity)
        var isInterrupted = false

        val delayStartTime = System.currentTimeMillis()
        clock = doAsync {
            while (System.currentTimeMillis() - delayStartTime < delaySecs * 1000 && preStartDelayRunning && !isInterrupted) {
                val diffSecs = (System.currentTimeMillis() - delayStartTime) / 1000
                val time = delaySecs - diffSecs
                if (ma.getStartStopSoundFlag())
                    if (time == 1.toLong() || time == 2.toLong()) preStartTone()

                val str = "Start in ${time}s"
                uiThread {
                    clockViewX.text = str
                }
                isInterrupted = sleep(1000)
            }
            if (!isInterrupted) {

                if (preStartDelayRunning)
                    uiThread {
                        doStartNow()
                    }
            }
        }

    }

    private fun doStartNow() {

        val ma = (activity as MainActivity)
        val raceDurationMins = ma.getRaceDurationValueFlag()
        val limitRaceDuration = ma.getRaceDurationFlag()
        val includeMinLapTime = ma.getIncludeMinLapTimeFlag()
        val minLapTime = ma.getMinLapTimeFlag()

        val totalRaceTime = if (includeMinLapTime) raceDurationMins * 60 + minLapTime else raceDurationMins * 60

        (timingContentView.adapter as RacingModeRecyclerViewAdapter).clearResults()
        preStartDelayRunning = false
        raceRunning = true
        startStopButtonM?.text = getText(R.string.stop)
        val racingStartTime = System.currentTimeMillis()
        var isInterrupted = false
        clock = doAsync {

            if (ma.getStartStopSoundFlag()) startTone()

            if (limitRaceDuration) {
                while ((System.currentTimeMillis() - racingStartTime) / 1000 <= totalRaceTime && raceRunning && !isInterrupted) {
                    val timeMs = System.currentTimeMillis() - racingStartTime
                    val str = "${Tools.millisToTimeWithMillis(timeMs)} / ${Tools.millisToTime((totalRaceTime * 1000).toLong())}"
                    uiThread {
                        clockViewX.text = str
                    }
                    isInterrupted = sleep(30)
                }
                if (!isInterrupted) {
                    uiThread {
                        doStop()
                        clockViewX.text = Tools.millisToTimeWithMillis((totalRaceTime * 1000).toLong())
                    }
                }

            } else {
                while (raceRunning && !isInterrupted) {
                    val timeMs = System.currentTimeMillis() - racingStartTime
                    val str = Tools.millisToTimeWithMillis(timeMs)
                    uiThread {
                        clockViewX.text = str
                    }
                    isInterrupted = sleep(30)
                }
                if (!isInterrupted) {
                    uiThread {
                        doStop()
                        clockViewX.text = Tools.millisToTimeWithMillis((totalRaceTime * 1000).toLong())
                    }
                }
            }
        }
        Log.i(TAG, "Loop done")
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(receiver)
        clock?.cancel(true)
    }

    fun isRaceRunning(): Boolean {
        return preStartDelayRunning || raceRunning
    }


    companion object {

        const val TAG = "RacingModeFragment"

        @JvmStatic
        fun newInstance() = RacingModeFragment()
    }
}
