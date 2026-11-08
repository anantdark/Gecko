package com.anant.gecko.data

import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gecko")

class GeckoPreferences(private val context: Context) {

    val listeningEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_LISTENING] ?: true
    }

    val memoryBytes: Flow<Long> = context.dataStore.data.map {
        it[KEY_MEMORY_BYTES] ?: defaultMemoryBytes()
    }

    val sampleRate: Flow<Int> = context.dataStore.data.map {
        it[KEY_SAMPLE_RATE] ?: DEFAULT_SAMPLE_RATE
    }

    val themeId: Flow<ThemeId> = context.dataStore.data.map {
        ThemeId.fromKey(it[KEY_THEME])
    }

    val iconMorph: Flow<IconMorph> = context.dataStore.data.map {
        IconMorph.fromKey(it[KEY_ICON_MORPH])
    }

    val iconMorphSchedule: Flow<IconMorphSchedule> = context.dataStore.data.map {
        IconMorphSchedule.fromKey(it[KEY_ICON_MORPH_SCHEDULE])
    }

    suspend fun isListeningEnabled(): Boolean = listeningEnabled.first()
    suspend fun getMemoryBytes(): Long = memoryBytes.first()
    suspend fun getSampleRate(): Int = sampleRate.first()
    suspend fun getThemeId(): ThemeId = themeId.first()
    suspend fun getIconMorph(): IconMorph = iconMorph.first()
    suspend fun getIconMorphSchedule(): IconMorphSchedule = iconMorphSchedule.first()
    suspend fun getIconMorphDayStamp(): Int = context.dataStore.data.first()[KEY_ICON_MORPH_DAY] ?: -1

    suspend fun setListeningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LISTENING] = enabled }
    }

    suspend fun setMemoryBytes(bytes: Long) {
        // Store the user's pick as-is; heap cap is applied only when allocating the buffer.
        context.dataStore.edit { it[KEY_MEMORY_BYTES] = bytes.coerceAtLeast(1L) }
    }

    suspend fun setSampleRate(rate: Int) {
        context.dataStore.edit { it[KEY_SAMPLE_RATE] = rate }
    }

    suspend fun setThemeId(id: ThemeId) {
        context.dataStore.edit { it[KEY_THEME] = id.storageKey }
    }

    suspend fun setIconMorph(morph: IconMorph) {
        context.dataStore.edit { it[KEY_ICON_MORPH] = morph.storageKey }
    }

    suspend fun setIconMorphSchedule(schedule: IconMorphSchedule) {
        context.dataStore.edit { it[KEY_ICON_MORPH_SCHEDULE] = schedule.storageKey }
    }

    suspend fun setIconMorphDayStamp(day: Int) {
        context.dataStore.edit { it[KEY_ICON_MORPH_DAY] = day }
    }

    fun defaultMemoryBytes(): Long = DEFAULT_MEMORY_BYTES

    companion object {
        /** 16 kHz — clear speech with a modest PCM cost over 8 kHz. */
        const val DEFAULT_SAMPLE_RATE = 16_000

        /** 256 MB rolling buffer (capped to 90% of the app heap on small devices). */
        const val DEFAULT_MEMORY_BYTES = 256L * 1024 * 1024

        val QUALITY_OPTIONS = listOf(8_000, 16_000, 44_100)

        val MEMORY_OPTIONS = listOf(
            128L * 1024 * 1024,
            256L * 1024 * 1024,
            512L * 1024 * 1024,
        )

        private val KEY_LISTENING = booleanPreferencesKey("listening_enabled")
        private val KEY_MEMORY_BYTES = longPreferencesKey("memory_bytes")
        private val KEY_SAMPLE_RATE = intPreferencesKey("sample_rate")
        private val KEY_THEME = stringPreferencesKey("theme_id")
        private val KEY_ICON_MORPH = stringPreferencesKey("icon_morph")
        private val KEY_ICON_MORPH_SCHEDULE = stringPreferencesKey("icon_morph_schedule")
        private val KEY_ICON_MORPH_DAY = intPreferencesKey("icon_morph_day")

        fun memoryHeapCap(): Long = (Runtime.getRuntime().maxMemory() * 0.90).toLong()

        /** Clamp for AudioMemory allocation only — preferences keep the user's chosen size. */
        fun cappedMemory(bytes: Long): Long = bytes.coerceAtMost(memoryHeapCap()).coerceAtLeast(1L)

        fun nativeSampleRate(): Int =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
    }
}
