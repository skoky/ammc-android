package com.skoky.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import com.skoky.Const.minLapTimeK
import com.skoky.Const.raceDurationValueK
import com.skoky.Const.startupDelayValueK
import com.skoky.MainActivity
import com.skoky.MyApp
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.fragment_options.*

class MyTextWatcher(val ma: MainActivity, val key: String, val value: EditText) : TextWatcher {
    override fun afterTextChanged(p0: Editable?) {}
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        ma.saveIntValue(value, key)
    }
}

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


        activity?.let { a ->
            val app = a.application as MyApp
            val ma = (a as MainActivity)

            checkStartupDelay.isChecked = ma.getStartupDelayFlag()
            ma.showHideStartupDelayValue(checkStartupDelay.isChecked)
            checkRaceDuration.isChecked = ma.getRaceDurationFlag()
            ma.showHideRaceDurationValue(checkRaceDuration.isChecked)
            checkIncludeMinLapTime.isChecked = ma.getIncludeMinLapTimeFlag()

            a.findViewById<EditText>(R.id.checkMinLapTime)?.let {
                it.setText(ma.getMinLapTimeFlag().toString())
                it.addTextChangedListener(MyTextWatcher(ma, minLapTimeK, it))
            }
            a.findViewById<EditText>(R.id.startupDelayValue)?.let {
                it.setText(ma.getStartupDelayValueFlag().toString())
                it.addTextChangedListener(MyTextWatcher(ma, startupDelayValueK, it))
            }

            a.findViewById<EditText>(R.id.raceDurationValue)?.let {
                it.setText(ma.getRaceDurationValueFlag().toString())
                it.addTextChangedListener(MyTextWatcher(ma, raceDurationValueK, it))
            }

            a.findViewById<CheckBox>(R.id.checkDriverSync)?.isChecked = ma.getDriverSyncFlag()
            a.findViewById<CheckBox>(R.id.checkBadMsg)?.isChecked = ma.getBadMsgFlag()
            a.findViewById<CheckBox>(R.id.checkTransponderSound).isChecked = ma.getTransponderSoundFlag()
            a.findViewById<CheckBox>(R.id.checkStartStopSound).isChecked = ma.getStartStopSoundFlag()
        }
    }

    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: ConsoleModel?)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.clearFindViewByIdCache()
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
