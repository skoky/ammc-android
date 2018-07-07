package com.skoky.fragment.content


data class Lap(val number: Int, val time: Long, val lapTimeMs: Int, val diff: Float)

class TrainingModeModel {

    var lastTime = 0
    private val allTransponders = mutableSetOf<Int>()
    private var myTransponder: Int? = null

    fun newPassing(values: List<Lap>, transponder: Int, time: Long): MutableList<Lap> {

        if (allTransponders.size == 0) myTransponder = transponder
        if (!allTransponders.contains(transponder)) allTransponders.add(transponder)

        if (transponder != myTransponder) return values.toMutableList()

        return if (values.isEmpty())
            mutableListOf(Lap(1, time, 0, 0f))
        else {
            val sorted = values.sortedByDescending { it.number }

            val lastLap = sorted.first()

            val newLastTime = Lap(lastLap.number, lastLap.time,
                    ((time - lastLap.time) / 1000).toInt(), 1f)

            val nowLastLap = Lap(lastLap.number + 1, time, 0, 0f)


            val x = mutableListOf<Lap>(nowLastLap, newLastTime)
            val rest = values.drop(1)
            x.addAll(rest)
            return if (x.size > MAX_SIZE)
                x.dropLast(x.size - MAX_SIZE).toMutableList()
            else
                x
        }
    }

    companion object {
        private const val MAX_SIZE = 5
    }
}