package com.skoky.timing


import android.app.Activity
import android.util.Log
import com.skoky.config.ConfigTool

import java.util.*

class ModelHandler(a: Activity) {

    private var records = HashMap<Int, TimingRecord>(20)
    private var lastPassingN = 0
    private val minLapTime: Int = ConfigTool.minLapTime

    private val mc = ModelComparator()

    val sortedModel: List<*>
        get() {

            val sortedModel = Vector<TimingRecord>(10)
            val i = records.values.iterator()
            while (i.hasNext()) {
                sortedModel.add(i.next())
            }
            Collections.sort<TimingRecord>(sortedModel, mc)

            return sortedModel
        }

    fun update(record: String): TimingRecord? {

//        val detail = record as PassingGeneric ?: return null

//        val driverId = detail.transponder
//        var dr: TimingRecord? = null

//        if (isValidRecordPassing(detail) == null) {
//            if (records.containsKey(driverId)) { // update record
//                dr = records[driverId]
//                if (isValidRecordMinLapTime(detail, dr)) {
//                    dr = updateRecord(dr, detail)
//                } else
//                    return null
//
//            } else {  // add new record
//                dr = TimingRecord(99, driverId, detail.passingNumber)
//                dr.lastRTCTime = detail.rtcTime
//            }
//            records[dr!!.transId] = dr
//        } else
//            return null
//
//        return dr
        return null  // nic
    }

    private fun isValidRecordPassing(detail: String): String? {
        // TODO: opakovani kdyz je preskoceny passing N
        var msg: String? = null
        if (lastPassingN == 0)
            lastPassingN = 1 // detail.passingNumber
        else if (lastPassingN + 1 == 0 ) // detail.passingNumber)
            msg = null
        else {
            msg = "Invalid passing number"
            Log.e(TAG, "Invalid passingN. Last:" + lastPassingN + " current:" + 1 ) // detail.passingNumber)
        }
        lastPassingN = 1 // detail.passingNumber
        return msg
    }

    private fun isValidRecordMinLapTime(detail: String, tr: TimingRecord?): Boolean {

        val lapTime = countLapTime(detail, tr!!)
        if (lapTime < minLapTime * 1000) {
            Log.e(TAG, "Lap time lower than min. lap time - " + lapTime + "ms")
            return false
        }

        return true
    }

    private fun updateRecord(dr: TimingRecord?, record: String): TimingRecord? {
        val lapTime = countLapTime(record, dr!!)
//        if (dr.lastRTCTime == null) {  // first record
//            dr.lastRTCTime = 1 // record.rtcTime
//        } else if (record.rtcTime > dr!!.lastRTCTime!!) {
//            dr.lastTimeMs = lapTime
//        } else {
//            Log.e(TAG, "Invalid lap time < 0$lapTime")
//            return null
//        }
//        if (lapTime < dr.bestTimeMs || dr.bestTimeMs == 0L)
//            dr.bestTimeMs = lapTime
//        dr.laps = dr.laps + 1
//        dr.lastPassingN = record.passingNumber
//        dr.lastRTCTime = record.rtcTime
//        dr.timestamp = System.currentTimeMillis()
        return dr
    }

    private fun countLapTime(record: String, dr: TimingRecord): Long {

//        return if (dr.lastRTCTime == null) 0 else record.rtcTime!! - dr!!.lastRTCTime!!
        return 1
    }

    fun clearResults() {
        records.clear()
    }

    private inner class ModelComparator : Comparator<TimingRecord> {
        override fun compare(o1: TimingRecord, o2: TimingRecord): Int {

            val t1: TimingRecord = o1
            val t2: TimingRecord = o2

            return if (t1.laps > t2.laps)
                -1
            else if (t1.laps < t2.laps)
                1
            else {
                if (t1.lastRTCTime == null) return 1
                if (t2.lastRTCTime == null) return -1
                if (t1.lastRTCTime!! > t2.lastRTCTime!!)
                    -1
                else if (t1.lastRTCTime!! < t2.lastRTCTime!!)
                    1
                else
                    0
            }
        }
    }

    companion object {
        private const val TAG = "ModelHandler"
    }

}
