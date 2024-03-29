package com.skoky

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder


const val DELIM = "\t"
const val VOSTOK_ID = "101"
const val VOSTOK_NAME = "Vostok"

data class Status(
    val msg: String,
    val decoderType: String,
    val decoderId: String,
    val packetSequenceNum: Int,
    val noise: Int,
    val crcOk: Boolean
)

data class Passing(
    val msg: String,
    val decoderType: String,
    val decoderId: String,
    val packetSequenceNum: Int,
    val transponderCode: String,
    val timeSinceStart: Float,
    val hitCounts: Int,
    val signalStrength: Int,
    val passingStatus: Int,
    val crcOk: Boolean,
    val msecs_since_start: Long
)

data class Error(val msg: String, val description: String)

val gson = GsonBuilder().setPrettyPrinting().create()

const val TAG = "P98Parser"

object P98Parser {

    fun myGson() =
        GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    fun parse(msgMaybeMore: ByteArray, id: String): String {

        // FIXME parse all messages, not only first!
        val msg = String(msgMaybeMore).split("\r\n").first().toByteArray()

        Log.i(TAG,String(msg))
        return try {
            when (msg[0].toInt().toChar()) {
                '#' -> parserStatus(msg.copyOfRange(1, msg.size - 1), id)
                '@' -> parsePassing(msg.copyOfRange(1, msg.size - 1), id)
                else -> makeError("unknown 98 record type ${String(msg)}")
            }
        } catch (e: Exception) {
            makeError("${e.message}")
        }
    }

    private fun parserStatus(msg: ByteArray, id: String): String {
        val fields = String(msg).split(DELIM)
        if (fields.size < 5) return makeError(
            "Status does not have 5 fields - ${fields.size} / ${
                String(
                    msg
                )
            }"
        )
        val isCrcOk = checkCrc(msg, fields[4])
        val type = if (fields[1] == VOSTOK_ID) VOSTOK_NAME else ""
        val status = Status(
            msg = "STATUS",
            decoderType = type,
            decoderId = id,
            packetSequenceNum = fields[2].toInt(),
            noise = fields[3].toInt(),
            crcOk = isCrcOk
        )
        return myGson().toJson(status)
    }

    private fun parsePassing(msg: ByteArray, id: String): String {

        val fields = String(msg).split(DELIM)
        if (fields.size != 9) return makeError("Passing does not have 9 fields , ${fields.size}")

        val isCrcOk = checkCrc(msg, fields[8])
        val type = if (fields[1] == VOSTOK_ID) VOSTOK_NAME else ""
        val tss = fields[4].toFloat()
        val passing = Passing(
            msg = "PASSING",
            decoderType = type,
            decoderId = id,
            packetSequenceNum = fields[2].toInt(),
            transponderCode = fields[3],
            timeSinceStart = tss,
            hitCounts = fields[5].toInt(),
            signalStrength = fields[6].toInt(),
            passingStatus = fields[7].toInt(),
            crcOk = isCrcOk,
            msecs_since_start = (tss * 1000).toLong()  // conversion to millis since start
        )

        return myGson().toJson(passing)
    }

    private fun makeError(msg: String): String {
        return myGson().toJson(Error("Error", msg))
    }


    private fun checkCrc(_msg: ByteArray, _crc: String): Boolean {
        return true
    }

    // does not work
//    //        receivedCrc = Integer.parseInt(ps.getCrc().replaceAll("x", ""), 16).toShort()
//    //        if (crcCalc === receivedCrc) ps.setCrcOk(true)
//
//    fun calcCrc(data: ByteArray): Short {
//        val dataToCrc = ByteArray(data.size - 6)
//        System.arraycopy(data, 1, dataToCrc, 0, dataToCrc.size)
//        return CRC16.cmpCRC(dataToCrc)
//    }


}
