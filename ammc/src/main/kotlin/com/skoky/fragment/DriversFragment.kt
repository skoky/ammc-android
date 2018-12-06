package com.skoky.fragment

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import org.jetbrains.anko.childrenSequence

data class DriverPair(val t: String, val d: String)

class DriversFragment : FragmentCommon() {

    private var listener: OnListFragmentInteractionListener? = null

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    private val rows = mutableMapOf<String, LinearLayout>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val enabled = (activity!!.application as MyApp).options["driversync"] as Boolean
        if (!enabled) {
            Toast.makeText(activity, "Enabled Cloud sync to use Drivers Editor", Toast.LENGTH_LONG).show()
            val a = (activity as MainActivity)
            a.openStartupFragment()
            return null
        }
        val view = inflater.inflate(R.layout.fragment_drivers_list, container, false)

        view.findViewById<ImageView>(R.id.addDriverImage).setOnClickListener { addNewDriverHandler(it) }
        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        val app = activity!!.application as MyApp
        val transponders = app.recentTransponders.map { DriverPair(it, "") }.toMutableList()

        transponders.forEach { p ->
            addRow(inflater, container, app, ll, p, true, activity!!)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val enabled = (activity!!.application as MyApp).options["driversync"] as Boolean
        if (!enabled) return

        val app = activity!!.application as MyApp
        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout
        app.drivers.driversList() { t, d ->
            Log.d(TAG, "Driver $t $d")
            if (t.isEmpty() && d.isEmpty()) {
                view.findViewById<ProgressBar>(R.id.progressBarDrivers).visibility = GONE
            } else {
                if (rows.containsKey(t)) {
                    (rows[t]!!.getChildAt(1) as EditText).setText(d)
                    (rows[t]!!.getChildAt(1) as EditText).tag = false
                } else
                    activity?.let { a ->
                        addRow(LayoutInflater.from(a), view, app, ll, DriverPair(t, d), false, a)
                    }

            }
        }
    }

    private fun addNewDriverHandler(view: View) {
        val app = activity!!.application as MyApp
        val sv = activity!!.findViewById<ScrollView>(R.id.sv)
        val ll = activity!!.findViewById<LinearLayout>(R.id.driversList)
        addRow(LayoutInflater.from(activity), sv, app, ll, DriverPair("", ""), true, activity!!)
    }

    private fun saveDriverName(t: String, v: View) {
        Log.d(TAG, "Driver tag ${v.tag}")
        if (t.trim().isNotEmpty() && v.tag != false) {
            val driverNameToSave = (v as EditText).text.toString()
            if (driverNameToSave.trim().isNotEmpty()) {
                (activity!!.application as MyApp).drivers.saveTransponder(t, driverNameToSave) { err ->
                    if (err.isEmpty()) {
                        val et = v as EditText
                        et.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_check_24px, 0)
                        Handler().postDelayed({
                            et.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        }, 2000)
                    }
                }
            }
        }
    }

    private fun addRow(inflater: LayoutInflater, container: ViewGroup?, app: MyApp, ll: LinearLayout,
                       p: DriverPair, top: Boolean, context: Context) {
        val newView = inflater.inflate(R.layout.drivers_line, container, false) as LinearLayout
        (newView.getChildAt(0) as EditText).setText(p.t)
        val edt = (newView.getChildAt(0) as EditText)
        (newView.getChildAt(1) as EditText).setText(p.d)
        val edd = (newView.getChildAt(1) as EditText)
        edd.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) saveDriverName(edt.text.toString(), v)
        }
        edd.tag = false
        edd.addTextChangedListener(EditTextWatcher(edd))

        newView.findViewById<ImageView>(R.id.deleteImage).setOnClickListener {

            if (edd.text.toString().trim().isNotEmpty()) {
                AlertDialog.Builder(context)
                        .setTitle("Are you sure to delete name to transponder ${p.t}?")
                        .setPositiveButton(R.string.yes) { _, _ ->
                            app.drivers.deleteAfterYes(p.t) {
                                ll.removeView(newView)
                                Log.d(TAG, "Driver view removed")
                            }
                        }
                        .setNegativeButton(R.string.no) { dialog, _ ->
                            dialog.dismiss()
                        }.create().show()
            }
        }
        if (top) ll.addView(newView, 1) else ll.addView(newView)
        rows[p.t] = newView
    }

    class EditTextWatcher(private val editText: EditText) : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            editText.tag=true
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

    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: ConsoleModel?)
    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "DriversFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                DriversFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
