package com.skoky.dummy

import java.util.ArrayList
import java.util.HashMap

object DummyContent {

    val ITEMS: MutableList<DummyItem> = ArrayList()
    val ITEM_MAP: MutableMap<String, DummyItem> = HashMap()

    init {
        addItem(DummyItem("1", "Single use more","D1"))
        addItem(DummyItem("2", "Racing mode","D2"))
        addItem(DummyItem("3", "Console mode", "D3"))
        addItem(DummyItem("4", "Decoders","D4"))
    }

    private fun addItem(item: DummyItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

//    private fun createDummyItem(position: Int): DummyItem {
//        return DummyItem(position.toString(), "Item " + position, makeDetails(position))
//    }
//
//    private fun makeDetails(position: Int): String {
//        val builder = StringBuilder()
//        builder.append("Details about Item: ").append(position)
//        for (i in 0..position - 1) {
//            builder.append("\nMore details information here.")
//        }
//        return builder.toString()
//    }

    /**
     * A dummy item representing a piece of content.
     */
    data class DummyItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}
