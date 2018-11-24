package com.skoky.fragment.content

import com.skoky.fragment.Time

data class TrainingLap(val number: Int, val time: Time, val lapTimeMs: Int, val diffMs: Int?)

class TrainingModeModel {

    private val allTransponders = mutableSetOf<String>()
    private var myTransponder: String? = null

    fun newPassing(values: List<TrainingLap>, transponder: String, time: Time): List<TrainingLap> {

        if (allTransponders.size == 0) myTransponder = transponder
        if (!allTransponders.contains(transponder)) allTransponders.add(transponder)

        if (transponder != myTransponder) return values.toMutableList()

        return if (values.isEmpty())
            mutableListOf(TrainingLap(0, time, 0, null))
        else {
            val sorted = values.sortedByDescending { it.number }.toMutableList()

            val lastLap = sorted.first()

            val lapTimeMs = ((time.us - lastLap.time.us).toInt()) / 1000
            val diff = (lapTimeMs - lastLap.lapTimeMs)
            val veryLastLap = TrainingLap(lastLap.number + 1, time, lapTimeMs, diff)

            sorted.add(veryLastLap)
            val newV = if (sorted.size > MAX_SIZE)
                sorted.drop(sorted.size - MAX_SIZE).toMutableList()
            else
                sorted
            return newV.sortedByDescending { it.number }
        }
    }

    fun setSelectedTransponder(transponder: String) {
        myTransponder = transponder
    }

    fun getSelectedTransponder(): String? {
        return myTransponder
    }

    companion object {
        private const val TAG = "TrainingModeModel"
        private const val MAX_SIZE = 5000
    }
}