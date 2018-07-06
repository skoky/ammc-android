package com.skoky.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.skoky.MyApp.Companion.getCachedApplicationContext
import com.skoky.OptionsActivity

object ConfigTool {
    private const val TAG = "ConfigTool"

    val storedAddress: String
        get() {
            val adr = prefs.getString("TCPIPADR", ":5403")
            Log.d(TAG, "Stored adr:" + adr!!)
            return adr
        }

    var raceTime: Int
        get() = prefs.getInt("RACE_TIME", OptionsActivity.DEFAULT_RACING_TIME)
        set(time) {
            val editor = prefs.edit()
            editor.putInt("RACE_TIME", time)
            editor.commit()
        }

    var minLapTime: Int
        get() = prefs.getInt("MIN_LAP_TIME", OptionsActivity.DEFAULT_MIN_LAP_TIME)
        set(minLapTime) {
            val editor = prefs.edit()
            editor.putInt("MIN_LAP_TIME", minLapTime)
            editor.commit()
        }

    val lastConnType: ConnTypeEnum
        get() {
            val prefs = prefs
            val connTypeS = prefs.getString("connType", ConnTypeEnum.TCPIP.name)
            return ConnTypeEnum.valueOf(connTypeS)
        }

    private val prefs: SharedPreferences
        get() = getCachedApplicationContext().getSharedPreferences("AMM", Context.MODE_PRIVATE)

    fun saveAddress(adr: String) {
        val e = prefs.edit()
        e.putString("TCPIPADR", adr)
        e.commit()
    }

    fun saveLastConnectionType(conn: ConnTypeEnum) {
        val prefs = prefs
        val edit = prefs.edit()
        edit.putString("connType", conn.name)
        edit.commit()
    }

}
