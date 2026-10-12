package com.anant.gecko.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.anant.gecko.data.GeckoPreferences
import kotlinx.coroutines.runBlocking

/**
 * Mic foreground services cannot legally start from BOOT_COMPLETED on modern Android
 * (ineligible FGS state). We only nudge when RECORD_AUDIO is already granted; if the OS
 * still rejects, [GeckoRecordingService] stays idle until the user opens the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            Log.i(TAG, "Boot: skip auto-listen — no RECORD_AUDIO")
            return
        }
        val enabled = runBlocking { GeckoPreferences(context).isListeningEnabled() }
        if (!enabled) return
        runCatching {
            ContextCompat.startForegroundService(
                context,
                GeckoRecordingService.startListeningIntent(context),
            )
        }.onFailure {
            Log.w(TAG, "Boot: could not start listening service", it)
        }
    }

    companion object {
        private const val TAG = "GeckoBoot"
    }
}
