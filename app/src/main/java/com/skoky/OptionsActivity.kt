package com.skoky

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.skoky.config.ConfigTool

class OptionsActivity : Activity(), View.OnFocusChangeListener {

    internal var config: ConfigTool? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.timingoptions)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            // ConfigTool.getRacingTime();
            val racingTime = ConfigTool.raceTime.toLong()
            var tv = findViewById(R.id.racingTimeText) as TextView
            tv.text = racingTime.toString()
            tv.onFocusChangeListener = this
            val minLapTime = ConfigTool.minLapTime
            tv = findViewById(R.id.minLapTimeText) as TextView
            tv.text = minLapTime.toString()
            tv.onFocusChangeListener = this
        }

    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {

    }

    companion object {
        val DEFAULT_RACING_TIME = 300
        val DEFAULT_MIN_LAP_TIME = 10
    }
    //
    //    public void onFocusChange(View view, boolean b) {
    //
    //        TextView tv = (TextView) view;
    //        boolean ok;
    //        if (!b) {  //leaving screen
    //            if (view.getId()==R.id.racingTimeText) {
    //                ok = Tools.INSTANCE.checkInt(tv.getText());
    //                if (ok) ConfigTool.setRaceTime(Integer.parseInt(tv.getText().toString()));
    //                else Tools.INSTANCE.toast(this,"Invalid number");
    //            } else if (view.getId()==R.id.minLapTimeText) {
    //                ok = Tools.INSTANCE.checkInt(tv.getText());
    //                if (ok) ConfigTool.setMinLapTime(Integer.parseInt(tv.getText().toString()));
    //                else Tools.INSTANCE.toast(this,"Invalid number");
    //            }
    //
    //
    //        }
    //
    //    }
}
