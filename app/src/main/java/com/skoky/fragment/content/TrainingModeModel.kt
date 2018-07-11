package com.skoky.fragment.content

data class Lap(val number: Int, val time: Long, val lapTimeMs: Int, val diff: Float)

class TrainingModeModel {

    private val allTransponders = mutableSetOf<Int>()
    private var myTransponder: Int? = null

    fun newPassing(values: List<Lap>, transponder: Int, time: Long): List<Lap> {

        if (allTransponders.size == 0) myTransponder = transponder
        if (!allTransponders.contains(transponder)) allTransponders.add(transponder)

        if (transponder != myTransponder) return values.toMutableList()

        return if (values.isEmpty())
            mutableListOf(Lap(1, time, -1, 0f))
        else {
            val sorted = values.sortedByDescending { it.number }.toMutableList()

            val lastLap = sorted.first()

            val lapTime = ((time - lastLap.time).toInt()) / 1000
            val diff = (lapTime - lastLap.lapTimeMs).toFloat() / 1000
            val veryLastLap = Lap(lastLap.number + 1, time, lapTime, diff)

            sorted.add(veryLastLap)
            val newV = if (sorted.size > MAX_SIZE)
                sorted.dropLast(sorted.size - MAX_SIZE).toMutableList()
            else
                sorted
            return newV.sortedByDescending { it.number }
        }
    }

    fun setSelectedTransponder(transponder: String) {
        myTransponder = transponder.toInt()
    }

    fun getSelectedTransponder(): Int? {
        return myTransponder
    }

    companion object {
        private const val TAG = "TrainingModeModel"
        private const val MAX_SIZE = 5
    }
}