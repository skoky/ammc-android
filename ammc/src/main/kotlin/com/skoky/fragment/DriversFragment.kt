package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import org.jetbrains.anko.childrenSequence

data class DriverPair(val t: String, val d: String)

class DriversFragment : Fragment() {

    private var listener: OnListFragmentInteractionListener? = null

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    private val rows = mutableMapOf<String, LinearLayout>()

    // TODO handle saving status

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_drivers_list, container, false)

        view.findViewById<ImageView>(R.id.addDriverImage).setOnClickListener { addNewDriverHandler(it) }
        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        val app = activity!!.application as MyApp
        val transponders = app.recentTransponders.map { DriverPair(it, "") }.toMutableList()

        transponders.forEach { p ->
            addRow(inflater, container, app, ll, p)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = activity!!.application as MyApp
        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        val dbDrivers = app.drivers.driversList() { t, d ->
            Log.d(TAG, "Driver $t $d")
            if (rows.containsKey(t)) {
                (rows[t]!!.getChildAt(1) as EditText).setText(d)
            } else
                addRow(LayoutInflater.from(activity), view, app, ll, DriverPair(t, d))
        }
    }

    private fun addNewDriverHandler(view: View) {
        val app = activity!!.application as MyApp
        val sv = activity!!.findViewById<ScrollView>(R.id.sv)
        val ll = activity!!.findViewById<LinearLayout>(R.id.driversList)
        addRow(LayoutInflater.from(activity), sv, app, ll, DriverPair("", ""))
    }

    private fun saveDriverName(t: String, v: View) {
        if (t.trim().isNotEmpty()) {
            val driverNameToSave = (v as EditText).text.toString()
            if (driverNameToSave.trim().isNotEmpty()) {
                (activity!!.application as MyApp).drivers.saveTransponder(t, driverNameToSave)
            }
        }
    }

    private fun addRow(inflater: LayoutInflater, container: ViewGroup?, app: MyApp, ll: LinearLayout,
                       p: DriverPair) {
        val newView = inflater.inflate(R.layout.drivers_line, container, false) as LinearLayout
        (newView.getChildAt(0) as EditText).setText(p.t)
        (newView.getChildAt(1) as EditText).setText(p.d)
        (newView.getChildAt(1) as EditText).setOnFocusChangeListener { v, hasFocus -> if (!hasFocus) saveDriverName(p.t, v) }
        newView.findViewById<ImageView>(R.id.deleteImage).setOnClickListener {
            app.drivers.delete(p.t) {
                ll.removeView(newView)
                Log.d(TAG, "Driver view removed")
            }
        }
        ll.addView(newView,1)
        rows[p.t] = newView
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

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "ConsoleModeFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                DriversFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
