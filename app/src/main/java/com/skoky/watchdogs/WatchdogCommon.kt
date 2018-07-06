package com.skoky.watchdogs

import android.util.Log
//import eu.plib.Ptools.Bytes
//import eu.plib.Ptools.ProtocolsEnum

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

open class WatchdogCommon : Thread() {

    @Throws(IOException::class)
    protected fun readRecord(`is`: InputStream): String? {
//
//        val buffer2 = ByteArrayOutputStream()
//
//        val nRead: Int
//        val data = ByteArray(1024)
//
//        nRead = `is`.read(data, 0, data.size)
//        buffer2.write(data, 0, nRead)
//        Log.v(TAG, "Bytes read:$nRead")
//
//        if (nRead == -1) {
//            Log.d(TAG, "Broken record from input")
//            return null
//        }
//        // TODO more records if (one == P3Processor.EOR) break;
//        if (nRead > 1000) {
//            Log.d(TAG, "Record too long. Error")
//            return null
//        }
//        val buffer = Bytes()
//        buffer.add(buffer2.toByteArray())
//        return buffer
        return "nic"
    }

    companion object {
        private val TAG = "Watchdog"
    }

}
