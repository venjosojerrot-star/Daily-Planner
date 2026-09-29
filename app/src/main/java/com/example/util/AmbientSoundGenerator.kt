package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AmbientSoundType {
    NONE, WHITE_NOISE, RAIN, FOCUS_BINAURAL
}

class AmbientSoundGenerator {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startSound(type: AmbientSoundType) {
        stopSound()
        if (type == AmbientSoundType.NONE) return

        playbackJob = scope.launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val buffer = ShortArray(minBufferSize)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            var phase = 0.0
            while (isActive) {
                for (i in buffer.indices) {
                    when (type) {
                        AmbientSoundType.WHITE_NOISE -> {
                            val noise = (Random.nextFloat() * 2f - 1f) * 0.08f
                            buffer[i] = (noise * Short.MAX_VALUE).toInt().toShort()
                        }
                        AmbientSoundType.RAIN -> {
                            // Soft filtered pink-ish rain sound
                            val white = Random.nextFloat() * 2f - 1f
                            val drop = if (Random.nextFloat() < 0.002f) (Random.nextFloat() * 0.3f) else 0f
                            val sample = (white * 0.04f + drop)
                            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                        }
                        AmbientSoundType.FOCUS_BINAURAL -> {
                            // Smooth 220Hz theta wave tone
                            phase += 2.0 * Math.PI * 220.0 / sampleRate
                            val tone = Math.sin(phase) * 0.06
                            buffer[i] = (tone * Short.MAX_VALUE).toInt().toShort()
                        }
                        else -> {
                            buffer[i] = 0
                        }
                    }
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stopSound() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
