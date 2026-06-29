package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class GameAudioSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null
    
    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                startBackgroundMusic()
            } else {
                stopBackgroundMusic()
            }
        }

    private val sampleRate = 22050

    fun playClick() {
        if (!isSoundEnabled) return
        scope.launch {
            generateTone(800.0, 40, 0.3)
        }
    }

    fun playJump() {
        if (!isSoundEnabled) return
        scope.launch {
            // Fast slide up in frequency
            val durationMs = 120
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = 300.0 + (t * 350.0) // 300Hz to 650Hz
                phase += (2.0 * Math.PI * freq) / sampleRate
                val amp = 0.4 * (1.0 - t) // Fade out
                buffer[i] = (sin(phase) * amp * Short.MAX_VALUE).toInt().toShort()
            }
            writeToAudioTrack(buffer)
        }
    }

    fun playDoubleJump() {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 150
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = 500.0 + (t * 450.0) // 500Hz to 950Hz
                phase += (2.0 * Math.PI * freq) / sampleRate
                val amp = 0.4 * (1.0 - t)
                buffer[i] = (sin(phase) * amp * Short.MAX_VALUE).toInt().toShort()
            }
            writeToAudioTrack(buffer)
        }
    }

    fun playCoin() {
        if (!isSoundEnabled) return
        scope.launch {
            // Arpeggio / High chime: 987Hz (B5) then 1318Hz (E6)
            generateTone(987.77, 80, 0.4)
            delay(40)
            generateTone(1318.51, 150, 0.4)
        }
    }

    fun playPowerUp() {
        if (!isSoundEnabled) return
        scope.launch {
            // Uplifting sequence: 440 -> 554 -> 659 -> 880
            val freqs = listOf(440.0, 554.37, 659.25, 880.0)
            for (freq in freqs) {
                generateTone(freq, 70, 0.3)
                delay(40)
            }
        }
    }

    fun playDamage() {
        if (!isSoundEnabled) return
        scope.launch {
            // Low rumble sweep down
            val durationMs = 250
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = maxOf(40.0, 250.0 - (t * 210.0)) // 250Hz down to 40Hz
                phase += (2.0 * Math.PI * freq) / sampleRate
                // Add some noise simulation
                val noise = (Math.random() * 2.0 - 1.0) * 0.15
                val signal = sin(phase) * 0.5 + noise
                val amp = 0.6 * (1.0 - t)
                buffer[i] = (signal * amp * Short.MAX_VALUE).toInt().toShort()
            }
            writeToAudioTrack(buffer)
        }
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        scope.launch {
            // Sad descending chord
            val freqs = listOf(392.00, 349.23, 311.13, 261.63) // G4, F4, Eb4, C4
            for (freq in freqs) {
                generateTone(freq, 150, 0.4)
                delay(100)
            }
        }
    }

    private fun generateTone(frequency: Double, durationMs: Int, volume: Double) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val angle = (2.0 * Math.PI * i * frequency) / sampleRate
                val envelope = if (t < 0.1) t / 0.1 else (1.0 - t) // Smooth attack and decay
                buffer[i] = (sin(angle) * volume * envelope * Short.MAX_VALUE).toInt().toShort()
            }
            writeToAudioTrack(buffer)
        } catch (e: Exception) {
            Log.e("AudioSynth", "Error generating tone", e)
        }
    }

    private fun writeToAudioTrack(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Release track once played
            scope.launch {
                delay(1000)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (ignore: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("AudioSynth", "Error writing audio buffer", e)
        }
    }

    fun startBackgroundMusic() {
        if (!isMusicEnabled) return
        if (musicJob != null && musicJob?.isActive == true) return

        musicJob = scope.launch(Dispatchers.Default) {
            // Retro 8-bit pentatonic melody loop
            val pentatonicScale = listOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25) // C4, D4, E4, G4, A4, C5
            val melody = listOf(0, 2, 3, 4, 3, 2, 0, 1, 2, 0, 3, 5, 4, 3, 2, 4)
            val bass = listOf(261.63, 329.63, 392.00, 440.00)
            
            var index = 0
            while (isActive) {
                if (isMusicEnabled) {
                    val noteFreq = pentatonicScale[melody[index % melody.size]]
                    val bassFreq = bass[(index / 4) % bass.size] / 2.0 // One octave lower
                    
                    // Synthesize double voice (melody + bass)
                    val noteDurationMs = 200
                    val numSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()
                    val buffer = ShortArray(numSamples)
                    
                    for (i in 0 until numSamples) {
                        val angleMelody = (2.0 * Math.PI * i * noteFreq) / sampleRate
                        val angleBass = (2.0 * Math.PI * i * bassFreq) / sampleRate
                        
                        // Square wave flavor for retro vibe, low-pass effect
                        val melSignal = if (sin(angleMelody) > 0) 0.1 else -0.1
                        val bassSignal = sin(angleBass) * 0.25
                        
                        val signal = melSignal + bassSignal
                        val t = i.toDouble() / numSamples
                        val envelope = if (t < 0.1) t / 0.1 else (1.0 - t)
                        
                        buffer[i] = (signal * envelope * Short.MAX_VALUE).toInt().toShort()
                    }
                    
                    try {
                        val track = AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            buffer.size * 2,
                            AudioTrack.MODE_STATIC
                        )
                        track.write(buffer, 0, buffer.size)
                        track.play()
                        
                        // Launch release
                        launch {
                            delay(500)
                            try {
                                track.stop()
                                track.release()
                            } catch (e: Exception) {}
                        }
                    } catch (e: Exception) {
                        Log.e("AudioSynth", "Melody error", e)
                    }
                }
                
                index++
                delay(220) // Speed of loop
            }
        }
    }

    fun stopBackgroundMusic() {
        musicJob?.cancel()
        musicJob = null
    }
}
