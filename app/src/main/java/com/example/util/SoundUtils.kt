package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

object SoundUtils {
    fun playClickSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {}
            }, 300)
        } catch (_: Exception) {}
    }

    fun playAccomplishedSound() {
        try {
            val volume = 85
            val handler = Handler(Looper.getMainLooper())
            
            // First note (low)
            val tone1 = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
            tone1.startTone(ToneGenerator.TONE_DTMF_1, 35)
            handler.postDelayed({ 
                try { tone1.release() } catch (_: Exception) {} 
            }, 45)
            
            // Second note (middle)
            handler.postDelayed({
                try {
                    val tone2 = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
                    tone2.startTone(ToneGenerator.TONE_DTMF_5, 35)
                    handler.postDelayed({ 
                        try { tone2.release() } catch (_: Exception) {} 
                    }, 45)
                } catch (_: Exception) {}
            }, 60)
            
            // Third note (high, lingering)
            handler.postDelayed({
                try {
                    val tone3 = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
                    tone3.startTone(ToneGenerator.TONE_DTMF_9, 120)
                    handler.postDelayed({ 
                        try { tone3.release() } catch (_: Exception) {} 
                    }, 140)
                } catch (_: Exception) {}
            }, 120)
        } catch (_: Exception) {}
    }
}
