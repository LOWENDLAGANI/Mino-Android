package com.minetallest.mino.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Lightweight haptic feedback utilities for touch interactions. */
object Haptics {

    /** Light tick for button taps and small interactions. */
    fun light(context: Context) {
        vibrate(context, amplitude = 15, duration = 10)
    }

    /** Medium tick for primary actions like sending a message. */
    fun medium(context: Context) {
        vibrate(context, amplitude = 25, duration = 20)
    }

    /** Soft impact for selection changes. */
    fun selection(context: Context) {
        vibrate(context, amplitude = 10, duration = 5)
    }

    private fun vibrate(context: Context, amplitude: Int, duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val vibrator = context.getSystemService(Vibrator.class)?.takeIf { it.canVibrate() }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, duration), intArrayOf(amplitude), -1))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Vibrator.class)
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Vibrator.class)
            vibrator?.vibrate(duration)
        }
    }
}
