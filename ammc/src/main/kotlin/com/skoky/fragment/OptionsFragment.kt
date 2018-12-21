package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import kotlinx.android.synthetic.main.fragment_options.*

class OptionsFragment : FragmentCommon() {

    private var listener: OnListFragmentInteractionListener? = null

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_options, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        activity?.let {
            val app = it.application as MyApp

            val startupDelay = app.options["startupDelay"] as? Boolean
            startupDelay?.let { d ->
                checkStartupDelay.isChecked = d
                startupDelayValue.setText(app.options["startupDelayValue"].toString())
                (activity as MainActivity).showHideStartupDelayValue(d)
            }

            startupDelayValue.addOnLayoutChangeListener { _: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
                (activity as MainActivity).saveStartupDelayValue(startupDelayValue)
            }

            it.findViewById<CheckBox>(R.id.checkDriverSync)?.isChecked = app.options["driversync"] as Boolean
            it.findViewById<CheckBox>(R.id.checkBadMsg)?.isChecked = app.options["badmsg"] as Boolean
        }

    }

    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: ConsoleModel?)
    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "OptionsFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                OptionsFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
