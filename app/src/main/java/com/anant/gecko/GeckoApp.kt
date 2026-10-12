package com.anant.gecko

import android.app.Application
import com.anant.gecko.data.GeckoPreferences
import com.anant.gecko.util.LauncherIconController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeckoApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Repair launcher aliases that persist across updates (disabled ≠ missing).
        appScope.launch {
            val prefs = GeckoPreferences(this@GeckoApp)
            LauncherIconController.ensureLauncherAlias(this@GeckoApp, prefs.getIconMorph())
        }
    }
}
