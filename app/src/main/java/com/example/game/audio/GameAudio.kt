package com.example.game.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GameAudio {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            // Priority high volume alarm or media track tone generator
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.e("GameAudio", "Failed to initialize ToneGenerator: ${e.message}")
        }
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun playClick() {
        audioScope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    fun playCoinCollected() {
        audioScope.launch {
            try {
                // High double beep
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
                delay(70)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
            } catch (e: Exception) { }
        }
    }

    fun playPowerUp() {
        audioScope.launch {
            try {
                // Cyber upward pitch scale
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 60)
                delay(80)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_4, 60)
                delay(80)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_7, 80)
            } catch (e: Exception) { }
        }
    }

    fun playLevelUp() {
        audioScope.launch {
            try {
                // A triumphant cyber-neon arpeggio
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_3, 100)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_6, 100)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 100)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 200)
            } catch (e: Exception) { }
        }
    }

    fun playCrash() {
        audioScope.launch {
            try {
                // Deep retro arcade rumble pattern
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_DIAL, 150)
                delay(180)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_LOW_L, 250)
            } catch (e: Exception) { }
        }
    }

    fun playShieldBreak() {
        audioScope.launch {
            try {
                // Deflection low-high dynamic beep
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_CONGESTION, 150)
            } catch (e: Exception) { }
        }
    }
}
