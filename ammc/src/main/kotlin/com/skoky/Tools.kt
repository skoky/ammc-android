package com.skoky


import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.util.Log
import com.skoky.fragment.Time

object Tools {

    private const val TAG = "P3Tools"

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
                newWakeLock(PARTIAL_WAKE_LOCK, "AMMC:LOCK").apply {
                    acquire(60 * 60 * 1000)
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

    fun timeToTextSpeech(lapTimeMs: Int, pattern: String): String {
        val millis = lapTimeMs % 1000
        val second = lapTimeMs / 1000 % 60
        val minute = lapTimeMs / (1000 * 60)

        val tts = pattern
            .replace("%ms", millis.toString(), false)
            .replaceFirst("%m", minute.toString(), false)
            .replace("%s", second.toString(), false)

        Log.i("TTS", tts)
        return tts

    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        val byteIterator = chunkedSequence(2)
            .map { it.toInt(16).toByte() }
            .iterator()

        return ByteArray(length / 2) { byteIterator.next() }
    }

    fun ByteArray.toHexString(): String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    fun rtcTimeFromString(rtc: String): Time {
        val t: String = AmmcBridge.time_to_millis(rtc)
        Log.i(TAG, "ttm: $rtc > $t")
        return try {
            val timeLong = t.toLong()
            Time(timeLong * 1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time", e)
            Time(0) // TODO better handling?
        }
    }

}
