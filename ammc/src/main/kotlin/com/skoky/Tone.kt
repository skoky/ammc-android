package com.skoky

import android.media.AudioManager.STREAM_MUSIC
import android.media.ToneGenerator
import android.media.ToneGenerator.TONE_CDMA_NETWORK_BUSY
import android.util.Log

object Tone {
    val TAG = Tone::class.simpleName

    fun stopTone() {
        playTone(TONE_CDMA_NETWORK_BUSY, 1000)
    }

    fun startTone() {
        playTone(ToneGenerator.TONE_DTMF_7, 600)
    }

    fun preStartTone() {
        playTone(ToneGenerator.TONE_DTMF_7, 200)
    }

    private fun playTone(tone: Int, duration: Int) {
        try {
            ToneGenerator(STREAM_MUSIC, 100).startTone(tone, duration)
        } catch (e: Exception) {
            Log.w(TAG, "Exception when playing tone", e)
        }
    }
}