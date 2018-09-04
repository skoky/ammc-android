package com.skoky.fragment.content

data class RacingLap(val number: Int, val timeUs: Long, val lapTimeMs: Int, val diffMs: Int?)

class RacingModeModel {

    private val allTransponders = mutableSetOf<Int>()

    fun newPassing(values: List<RacingLap>, transponder: Int, time: Long): List<RacingLap> {

        if (!allTransponders.contains(transponder)) allTransponders.add(transponder)

//        if (transponder != myTransponder) return values.toMutableList()

        return if (values.isEmpty())
            mutableListOf(RacingLap(0, time, 0, null))
        else {
            val sorted = values.sortedByDescending { it.number }.toMutableList()

            val lastLap = sorted.first()

            val lapTime = ((time - lastLap.timeUs).toInt()) / 1000
            val diff = (lapTime - lastLap.lapTimeMs)
            val veryLastLap = RacingLap(lastLap.number + 1, time, lapTime, diff)

            sorted.add(veryLastLap)
            val newV = if (sorted.size > MAX_SIZE)
                sorted.drop(sorted.size - MAX_SIZE).toMutableList()
            else
                sorted
            return newV.sortedByDescending { it.number }
        }
    }


    companion object {
        private const val TAG = "RacingModeModel"
        private const val MAX_SIZE = 5000
    }
}