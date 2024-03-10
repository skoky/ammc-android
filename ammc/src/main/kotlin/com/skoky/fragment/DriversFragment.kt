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
import androidx.core.view.children
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R

data class DriverPair(val t: String, val d: String)

class DriversFragment : FragmentCommon() {

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    private val rows = mutableMapOf<String, LinearLayout>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_drivers_list, container, false)

        activity?.let { _ ->
            val enabled = (activity as MainActivity).getDriverSyncFlag()
            if (!enabled) {
                Toast.makeText(activity, "Enabled Cloud sync to use Drivers Editor", Toast.LENGTH_LONG).show()
                val a = (activity as MainActivity)
                a.openStartupFragment()
                return null
            }

            view.findViewById<ImageView>(R.id.addDriverImage).setOnClickListener { addNewDriverHandler(it) }
            val ll = (view as ScrollView).children.first() as LinearLayout
            activity?.let { act2 ->
                val app = act2.application as MyApp
                val transponders = app.recentTransponders.map { DriverPair(it, "") }.toMutableList()

                transponders.forEach { p ->
                    addRow(inflater, container, app, ll, p, true, requireActivity())
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val enabled = (activity as MainActivity).getDriverSyncFlag()
        if (!enabled) return

        val ll = (view as ScrollView).children.first() as LinearLayout

        activity?.let { act ->
            val app = act.application as MyApp
            app.drivers.driversList { t, d ->
                Log.d(TAG, "Driver $t $d")

                if (t.isEmpty() && d.isEmpty()) { // end of loading
                    view.findViewById<ProgressBar>(R.id.progressBarDrivers).visibility = GONE
                } else {
                    if (rows.containsKey(t)) {
                        val rows2 = rows[t]
                        rows2?.let { r ->
                            (r.getChildAt(1) as EditText).setText(d)
                            (r.getChildAt(1) as EditText).tag = false
                        }
                    } else
                        activity?.let { a ->
                            addRow(LayoutInflater.from(a), view, app, ll, DriverPair(t, d), false, a)
                        }
                }
            }

        }
    }

    private fun addNewDriverHandler(view: View) {
        activity?.let { act ->
            val app = act.application as MyApp
            val sv = act.findViewById<ScrollView>(R.id.sv)
            val ll = act.findViewById<LinearLayout>(R.id.driversList)
            addRow(LayoutInflater.from(act), sv, app, ll, DriverPair("", ""), true, act)
        }
    }

    private fun saveDriverName(t: String, v: View) {
        Log.d(TAG, "Driver tag ${v.tag}")
        if (t.trim().isNotEmpty() && v.tag != false) {
            val driverNameToSave = (v as EditText).text.toString()
            if (driverNameToSave.trim().isNotEmpty()) {
                activity?.let { act ->
                    (act.application as MyApp).drivers.saveTransponder(t, driverNameToSave) { err ->
                        if (err.isEmpty()) {
                            v.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_check_24px, 0)
                            Handler().postDelayed({
                                v.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                            }, 2000)
                        }
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
            editText.tag = true
        }
    }

    companion object {

        const val TAG = "DriversFragment"

        @JvmStatic
        fun newInstance() = DriversFragment()
    }

}
