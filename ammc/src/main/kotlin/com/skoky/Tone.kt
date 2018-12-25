package com.skoky

import android.media.ToneGenerator
import android.media.ToneGenerator.TONE_CDMA_NETWORK_BUSY
import com.skoky.Wrapped.tone

object Tone {

    fun stopTone() {
        tone(TONE_CDMA_NETWORK_BUSY, 1000)
    }

    fun startTone() {
        tone(ToneGenerator.TONE_DTMF_7, 600)
    }

    fun preStartTone() {
        tone(ToneGenerator.TONE_DTMF_7, 200)
    }
}