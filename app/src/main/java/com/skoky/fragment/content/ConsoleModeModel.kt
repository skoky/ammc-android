package com.skoky.fragment.content

import com.skoky.fragment.Time

data class ConsoleModel(val decoderId: String)

class ConsoleModeModel {

    fun newPassing(values: List<ConsoleModel>, transponder: String, time: Time): List<ConsoleModel> {
        return mutableListOf(ConsoleModel("aaa"))
    }

    companion object {
        private const val TAG = "ConsoleModeModel"
    }
}