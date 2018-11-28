package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import org.jetbrains.anko.childrenSequence


class DriversFragment : Fragment() {

    private var listener: OnListFragmentInteractionListener? = null

    class ConnectionReceiver(val handler: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_drivers_list, container, false)

        val ll = (view as ScrollView).childrenSequence().first() as LinearLayout

        val app = activity!!.application as MyApp
        app.drivers.driversList() { t,d ->
            val newView = inflater.inflate(R.layout.drivers_line, container, false) as LinearLayout
            (newView.getChildAt(0) as EditText).setText(t)
            (newView.getChildAt(1) as EditText).setText(d)
            ll.addView(newView)
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
