package com.skoky.timing

import java.util.Date

class TimingRecord(position: Int, //    public int position;
                   var transId: Int, var lastPassingN: Int) {
    var racerName: String? = null
    var laps: Int = 0
    var lastRTCTime: Long? = null
    var bestTimeMs: Long = 0
    var lastTimeMs: Long = 0
    var timestamp: Long = 0

    init {
        this.laps = 0
        timestamp = System.currentTimeMillis()
    }//        this.position=position;

    override fun toString(): String {
        return " transId:" + transId + " laps:" + laps + "Last:" + lastTimeMs + " Time:" + timestamp
    }
}
