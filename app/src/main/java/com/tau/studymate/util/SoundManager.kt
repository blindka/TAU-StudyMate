package com.tau.studymate.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundManager {

    /**
     * צליל שמחה והצלחה נעים בעת השלמת מטלה (טון עולה + רטט קצר)
     */
    fun playTaskCompletedSound(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // רטט קצר של סיפוק והשלמה
                vibrate(context, longArrayOf(0, 40, 50, 70), intArrayOf(0, 150, 0, 255))

                // הפקת צליל הצלחה באמצעות ToneGenerator
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 220)
                delay(220)
                toneGen.release()
            } catch (e: Exception) {
                playSystemNotification(context)
            }
        }
    }

    /**
     * צליל התרעה לסיום טיימר למידה / פומודורו (3 צפצופים ברורים + רטט מעורר)
     */
    fun playTimerFinishedSound(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // רטט התרעה
                vibrate(context, longArrayOf(0, 180, 100, 180, 100, 250), intArrayOf(0, 220, 0, 220, 0, 255))

                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                for (i in 0 until 3) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
                    delay(320)
                }
                toneGen.release()
            } catch (e: Exception) {
                playSystemAlarm(context)
            }
        }
    }

    private fun playSystemNotification(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    private fun playSystemAlarm(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, -1)
                }
            }
        } catch (_: Exception) {}
    }
}
