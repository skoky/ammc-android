package com.skoky.watchdogs

//import eu.plib.Ptools.Message
import com.skoky.config.ConnTypeEnum

interface Watchdog {

//    val type: ConnTypeEnum
    fun getType() : ConnTypeEnum
    fun disconnect()
    fun send(msg: String): Boolean

    fun start()

    companion object {

        val CONNECTING = 1
        val CONNECTED = 2
        val RECORD = 3
        val CONNECTION_ERROR = 5
        val RECORD_INVALID = 6
        val CONNECTION_TIMEOUT = 7
        val INVALID_ADDRESS = 8
        val UNEXPECTED_ERROR = 9
        val CONNECTION_CLOSED = 10
        val RECORD_PARSED = 4
    }
}
