package com.skoky.fragment

import android.app.Application
import android.util.Log
import com.skoky.CloudDB
import com.skoky.MyApp
import org.json.JSONObject


data class Time(val us: Long)

open class FragmentCommon : android.support.v4.app.Fragment() {


    fun getTimeFromPassingJson(json: JSONObject): Time {
        return when {
            json.has("RTC_Time") -> Time((json.get("RTC_Time") as String).toLong())
            json.has("UTC_Time") -> Time((json.get("UTC_Time") as String).toLong())
            json.has("msecs_since_start") ->
                Time((json.get("msecs_since_start") as Integer).toLong() * 1000)
            else -> {
                Log.w(TrainingModeFragment.TAG, "No time in passing record $json")
                CloudDB.badMessageReport(activity!!.application as MyApp, "passing-no-time", json.toString())
                return Time(0L)
            }
        }
    }

    fun getTransponderFromPassingJson(app: Application, json: JSONObject): String {

        val transponderId = when {
            json.has("recentTransponders") -> (json.get("recentTransponders") as Int).toString()
            json.has("transponderCode") -> json.get("transponderCode") as String
            json.has("driverId") -> json.get("driverId") as String
            else -> {
                Log.w(TrainingModeFragment.TAG, "No racer identification in Passing $json")
                CloudDB.badMessageReport(activity!!.application as MyApp, "passing_not_transponder", json.toString())
                return "---"
            }
        }
//        (app as MyApp).drivers.saveNewTransponder(transponderId)
        return transponderId
    }
}