package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sound and Haptic feedback manager for FlashNotes.
 * Generates low-latency acoustic cues (shutter click, paper flip, success chime, penalty buzz)
 * and tactile haptics completely offline with zero binary assets.
 */
class SoundFeedbackManager private constructor(private val context: Context) {

    private var toneGen: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (e: Exception) {
            Log.w("SoundFeedbackManager", "Failed to initialize ToneGenerator", e)
        }
    }

    fun playShutterSound(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (hapticEnabled) performHapticTick(30)
        if (!soundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            } catch (e: Exception) {
                // Ignore tone playback errors
            }
        }
    }

    fun playCardFlipSound(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (hapticEnabled) performHapticTick(15)
        if (!soundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 25)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun playCorrectSound(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (hapticEnabled) performHapticSuccess()
        if (!soundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun playWrongSound(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (hapticEnabled) performHapticError()
        if (!soundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 90)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun playCelebrationSound(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (hapticEnabled) performHapticSuccess()
        if (!soundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
                delay(120)
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun performHapticTick(durationMs: Long = 20) {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic errors on emulators or devices without vibrator
        }
    }

    private fun performHapticSuccess() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    performHapticTick(40)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun performHapticError() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 40, 45), -1))
                } else {
                    performHapticTick(80)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        @Volatile
        private var instance: SoundFeedbackManager? = null

        fun getInstance(context: Context): SoundFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: SoundFeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
