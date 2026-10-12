package com.anant.gecko.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.anant.gecko.data.GeckoPreferences
import com.anant.gecko.data.IconMorph
import com.anant.gecko.data.IconMorphSchedule
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LauncherIconController {

    /** Manifest namespace — class names stay here even when applicationId has a .debug suffix. */
    private const val COMPONENT_PREFIX = "com.anant.gecko.icon."

    /**
     * Applies the scheduled morph. Prefer calling from [android.app.Activity.onStop]
     * so we never disable the launcher component that started the current process mid-frame.
     */
    suspend fun applyScheduledMorph(context: Context, prefs: GeckoPreferences) {
        withContext(Dispatchers.Default) {
            val schedule = prefs.getIconMorphSchedule()
            val next = when (schedule) {
                IconMorphSchedule.Fixed -> prefs.getIconMorph()
                IconMorphSchedule.EveryLaunch -> nextInCycle(prefs.getIconMorph())
                IconMorphSchedule.Daily -> {
                    val day = dayStamp()
                    if (day == prefs.getIconMorphDayStamp()) {
                        prefs.getIconMorph()
                    } else {
                        prefs.setIconMorphDayStamp(day)
                        nextInCycle(prefs.getIconMorph())
                    }
                }
            }
            setMorph(
                context = context,
                prefs = prefs,
                morph = next,
                persist = schedule != IconMorphSchedule.Fixed,
            )
        }
    }

    suspend fun setMorph(
        context: Context,
        prefs: GeckoPreferences,
        morph: IconMorph,
        persist: Boolean = true,
    ) {
        withContext(Dispatchers.Default) {
            if (persist) prefs.setIconMorph(morph)
            ensureLauncherAlias(context, morph)
        }
    }

    /**
     * Enables [target] first, then disables the other aliases.
     * Safe to call whenever the process is alive (e.g. Application.onCreate).
     */
    fun ensureLauncherAlias(context: Context, target: IconMorph) {
        val pm = context.packageManager
        val appId = context.packageName
        fun component(morph: IconMorph) =
            ComponentName(appId, COMPONENT_PREFIX + morph.aliasSuffix)

        // Enable target FIRST — disabling the active launcher alias before a replacement
        // is enabled kills the process / makes Studio report "does not exist".
        runCatching {
            pm.setComponentEnabledSetting(
                component(target),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        for (morph in IconMorph.entries) {
            if (morph == target) continue
            runCatching {
                pm.setComponentEnabledSetting(
                    component(morph),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    private fun nextInCycle(current: IconMorph): IconMorph {
        val all = IconMorph.entries
        val i = all.indexOf(current).coerceAtLeast(0)
        return all[(i + 1) % all.size]
    }

    private fun dayStamp(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
}
