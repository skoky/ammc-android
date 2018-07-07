package com.skoky.fragment.content

object TrainingModeModel {


    data class Lap(val number: Int, val time: Long, val lapTimeMs: Int, val diff: Float)

    val ITEMS: MutableList<Lap> = mutableListOf()

//    val ITEM_MAP: MutableMap<String, Lap> = HashMap()

    private val COUNT = 5

    init {
        ITEMS.sortedWith(compareByDescending { it.number })
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(Lap(i, i.toLong(), i*1000+i, i.toFloat()/10))
        }
    }

    private fun addItem(item: Lap) {
        ITEMS.add(item)
  //      ITEM_MAP.put(item.number.toString(), item)
    }

}
