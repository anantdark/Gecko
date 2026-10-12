package com.anant.gecko.util

import java.util.Locale
import kotlin.math.roundToInt

object TimeFormat {

    fun natural(seconds: Float): String {
        val total = seconds.coerceAtLeast(0f).roundToInt()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return when {
            hours > 0 && minutes > 0 ->
                "$hours ${plural(hours, "hour")} and $minutes ${plural(minutes, "minute")}"
            hours > 0 ->
                "$hours ${plural(hours, "hour")}"
            minutes > 0 && secs > 0 ->
                "$minutes ${plural(minutes, "minute")} and $secs ${plural(secs, "second")}"
            minutes > 0 ->
                "$minutes ${plural(minutes, "minute")}"
            else ->
                "$secs ${plural(secs, "second")}"
        }
    }

    fun shortTimer(seconds: Float): String {
        val total = seconds.coerceAtLeast(0f).roundToInt()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, secs)
        }
    }

    fun fileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format(Locale.US, "%.0f MB", mb)
            kb >= 1 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun plural(n: Int, unit: String): String =
        if (n == 1) unit else "${unit}s"
}
