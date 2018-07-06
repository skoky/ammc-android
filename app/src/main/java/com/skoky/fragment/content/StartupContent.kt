package com.skoky.fragment.content

import java.util.ArrayList
import java.util.HashMap

object StartupContent {
    val ITEMS: MutableList<DummyItem> = ArrayList()
    val ITEM_MAP: MutableMap<String, DummyItem> = HashMap()

    init {
        // Add some sample items.
            addItem(createDummyItem(1, "Decoder TBD"))
            addItem(createDummyItem(2, "Single"))
            addItem(createDummyItem(3, "Race mode"))
            addItem(createDummyItem(4, "Console mode"))
    }

    private fun addItem(item: DummyItem) {
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    private fun createDummyItem(position: Int, text: String): DummyItem {
        return DummyItem(position.toString(), text, makeDetails(position))
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0 until position) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    data class DummyItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}
