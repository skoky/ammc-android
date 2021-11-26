package com.skoky

import android.media.ToneGenerator
import android.media.ToneGenerator.*
import android.util.Log

object Tone {

    val TAG = Tone::class.simpleName

    fun stopTone(generator: ToneGenerator) {
        playTone(generator, TONE_CDMA_NETWORK_BUSY, 1000)
    }

    fun startTone(generator: ToneGenerator) {
        playTone(generator, TONE_DTMF_7, 600)
    }

    fun preStartTone(generator: ToneGenerator) {
        playTone(generator, TONE_DTMF_7, 200)
    }

    fun disconnectTone(generator: ToneGenerator) {
        playTone(generator, TONE_CDMA_NETWORK_BUSY_ONE_SHOT, 300)
    }

    private fun playTone(generator: ToneGenerator, tone: Int, duration: Int) {
        try {
            val t = generator.startTone(tone, duration)
            Log.d(TAG,"Played tone $t")
        } catch (e: Exception) {
            Log.w(TAG, "Exception when playing tone", e)
        }
    }


}

