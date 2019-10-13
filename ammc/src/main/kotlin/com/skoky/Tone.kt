package com.skoky

import android.media.AudioManager.STREAM_MUSIC
import android.media.ToneGenerator
import android.media.ToneGenerator.TONE_CDMA_NETWORK_BUSY

object Tone {


    fun stopTone() {
        ToneGenerator(STREAM_MUSIC, 100).startTone(TONE_CDMA_NETWORK_BUSY, 1000)
    }

    fun startTone() {
        ToneGenerator(STREAM_MUSIC, 100).startTone(ToneGenerator.TONE_DTMF_7, 600)
    }

    fun preStartTone() {
        ToneGenerator(STREAM_MUSIC, 100).startTone(ToneGenerator.TONE_DTMF_7, 200)
    }
}