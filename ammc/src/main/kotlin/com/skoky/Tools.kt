package com.skoky


import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK

object Tools {

    private val TAG = "P3Tools"

    var P3_DEF_PORT = 5403

    fun millisToTime(duration: Long): String {

        val seconds = (duration / 1000).toInt() % 60
        val minutes = (duration / (1000 * 60) % 60).toInt()

        return String.format("%d:%02d", minutes, seconds)
    }

    fun millisToTimeWithMillis(milliseconds: Long): String {

        val mils = milliseconds.toInt() % 1000
        val seconds = (milliseconds / 1000).toInt() % 60
        val minutes = (milliseconds / (1000 * 60) % 60).toInt()

        return String.format("%d:%02d.%03d", minutes, seconds, mils)

    }

    fun wakeLock(context: Context, b: Boolean) {
        if (b) {
            // FIXME use smaller wake lock on new androids
            MyApp.wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PARTIAL_WAKE_LOCK , "AMMC:LOCK").apply {
                    acquire()
                }

            }
        } else {
            MyApp.wakeLock?.let { lock ->
                if (lock.isHeld) lock.release()
            }
        }
    }

    fun timeToText(lapTimeMs: Int): String {
        val millis = lapTimeMs % 1000
        val second = lapTimeMs / 1000 % 60
        val minute = lapTimeMs / (1000 * 60)
        return String.format("%d:%d.%d", minute, second, millis)
    }
}
