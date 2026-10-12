package com.anant.gecko

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anant.gecko.data.GeckoPreferences
import com.anant.gecko.data.IconMorph
import com.anant.gecko.data.IconMorphSchedule
import com.anant.gecko.data.ThemeId
import com.anant.gecko.service.GeckoRecordingService
import com.anant.gecko.util.LauncherIconController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeckoViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = GeckoPreferences(app)
    private var service: GeckoRecordingService? = null
    private var serviceCollectJob: Job? = null

    private val _state = MutableStateFlow(GeckoRecordingService.UiState())
    val state: StateFlow<GeckoRecordingService.UiState> = _state.asStateFlow()

    private val _themeId = MutableStateFlow(ThemeId.Biolume)
    val themeId: StateFlow<ThemeId> = _themeId.asStateFlow()

    private val _iconMorph = MutableStateFlow(IconMorph.Green)
    val iconMorph: StateFlow<IconMorph> = _iconMorph.asStateFlow()

    private val _iconMorphSchedule = MutableStateFlow(IconMorphSchedule.Fixed)
    val iconMorphSchedule: StateFlow<IconMorphSchedule> = _iconMorphSchedule.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val s = (binder as GeckoRecordingService.LocalBinder).getService()
            service = s
            s.setUiAttached(true)
            serviceCollectJob?.cancel()
            serviceCollectJob = viewModelScope.launch {
                s.uiState.collect { _state.value = it }
            }
            // Always go through startListening so the service is startForegroundService'd
            // (not bind-only). Safe if already capturing.
            viewModelScope.launch {
                val wantListen = prefs.isListeningEnabled()
                if (wantListen) s.startListening()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service?.setUiAttached(false)
            service = null
            serviceCollectJob?.cancel()
        }
    }

    init {
        viewModelScope.launch {
            prefs.themeId.collect { _themeId.value = it }
        }
        viewModelScope.launch {
            prefs.iconMorph.collect { _iconMorph.value = it }
        }
        viewModelScope.launch {
            prefs.iconMorphSchedule.collect { _iconMorphSchedule.value = it }
        }
    }

    fun bind() {
        val ctx = getApplication<Application>()
        ctx.bindService(
            Intent(ctx, GeckoRecordingService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
    }

    fun unbind() {
        service?.setUiAttached(false)
        try {
            getApplication<Application>().unbindService(connection)
        } catch (_: Exception) {
        }
        service = null
    }

    fun toggleListen() {
        val s = service ?: return
        if (s.uiState.value.listeningEnabled &&
            s.uiState.value.mode != GeckoRecordingService.Mode.Idle
        ) {
            s.stopListening()
        } else {
            s.startListening()
        }
    }

    fun save(seconds: Float, name: String?) {
        service?.dumpRecording(seconds, name, null)
    }

    fun saveAndContinue(seconds: Float) {
        service?.startRecording(seconds)
    }

    fun stopRecording() {
        service?.stopRecording(null)
    }

    fun setMemory(bytes: Long) {
        service?.setMemoryBytes(bytes)
    }

    fun setSampleRate(rate: Int) {
        service?.setSampleRate(rate)
    }

    fun setTheme(id: ThemeId) {
        viewModelScope.launch { prefs.setThemeId(id) }
    }

    fun setIconMorph(morph: IconMorph) {
        viewModelScope.launch {
            // Manual picks only — auto schedules break home shortcuts on many OEMs.
            prefs.setIconMorphSchedule(IconMorphSchedule.Fixed)
            LauncherIconController.setMorph(getApplication(), prefs, morph)
        }
    }

    fun dismissSaved() {
        service?.clearSavedPrompt()
    }

    override fun onCleared() {
        unbind()
        super.onCleared()
    }
}
