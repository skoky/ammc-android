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
import com.skoky.Const.defaultTimeToSpeechPattern
import com.skoky.Const.minLapTimeK
import com.skoky.Const.raceDurationValueK
import com.skoky.Const.startupDelayValueK
import com.skoky.Const.timeToSpeechPattern
import com.skoky.MainActivity
import com.skoky.R

class MyTextWatcher(private val ma: MainActivity, val key: String, val value: EditText) : TextWatcher {
    override fun afterTextChanged(p0: Editable?) {}
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        ma.saveIntValue(value, key)
    }
}

class TestToSpeechPatternWatcher(private val ma: MainActivity, val key: String, var value: EditText) : TextWatcher {
    override fun afterTextChanged(p0: Editable?) {}
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (value.text.toString().isBlank()) {
            value.setText(defaultTimeToSpeechPattern)
        }
        ma.saveStringValue(value, key)
    }
}

class OptionsFragment : FragmentCommon() {

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
            val ma = (a as MainActivity)

            val sd = a.findViewById<CheckBox>(R.id.checkStartupDelay)
            sd.isChecked = ma.getStartupDelayFlag()
            ma.showHideStartupDelayValue(sd.isChecked)

            val rd = a.findViewById<CheckBox>(R.id.checkRaceDuration)
            rd.isChecked = ma.getRaceDurationFlag()
            ma.showHideRaceDurationValue(rd.isChecked)


            a.findViewById<CheckBox>(R.id.checkIncludeMinLapTime).isChecked = ma.getIncludeMinLapTimeFlag()

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


            val tts = a.findViewById<CheckBox>(R.id.checkTimeToSpeech)
            tts.isChecked = ma.getTimeToSpeechFlag()
            ma.showHideTimeToSpeechPattern(tts.isChecked)

            a.findViewById<EditText>(R.id.timeToSpeech)?.let {
                it.setText(ma.getTimeToSpeechPattern())
                it.addTextChangedListener(TestToSpeechPatternWatcher(ma, timeToSpeechPattern, it))
            }

            a.findViewById<CheckBox>(R.id.checkStartStopSound).isChecked = ma.getStartStopSoundFlag()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
// FIXME?        this.clearFindViewByIdCache()
    }

    companion object {

        const val TAG = "OptionsFragment"

        @JvmStatic
        fun newInstance() = OptionsFragment()
    }

}
