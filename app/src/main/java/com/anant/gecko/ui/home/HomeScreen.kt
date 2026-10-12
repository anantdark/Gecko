package com.anant.gecko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anant.gecko.R
import com.anant.gecko.service.GeckoRecordingService
import com.anant.gecko.ui.pack.GeckoBusyOverlay
import com.anant.gecko.ui.pack.GeckoButton
import com.anant.gecko.ui.pack.GeckoButtonTone
import com.anant.gecko.ui.pack.GeckoChipRow
import com.anant.gecko.ui.pack.GeckoDialog
import com.anant.gecko.ui.pack.GeckoField
import com.anant.gecko.ui.pack.GeckoIcon
import com.anant.gecko.ui.pack.GeckoIconButton
import com.anant.gecko.ui.pack.GeckoText
import com.anant.gecko.ui.pack.PackSurface
import com.anant.gecko.ui.theme.LocalGeckoTheme
import com.anant.gecko.util.TimeFormat
import java.io.File

@Composable
fun HomeScreen(
    state: GeckoRecordingService.UiState,
    onToggleListen: () -> Unit,
    onSave: (seconds: Float, name: String?) -> Unit,
    onSaveAndContinue: (seconds: Float) -> Unit,
    onStopRecording: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFile: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onDismissSaved: () -> Unit,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    var pendingSeconds by remember { mutableStateOf<Float?>(null) }
    var nameDraft by remember { mutableStateOf("") }

    val listening = state.listeningEnabled && state.mode != GeckoRecordingService.Mode.Idle
    val recording = state.mode == GeckoRecordingService.Mode.Recording

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 36.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GeckoText("Gecko", pack.type.brand, c.ink)
            GeckoIconButton(
                resId = R.drawable.ic_ph_gear,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                tint = c.inkMuted,
            )
        }

        GeckoText(
            "Rewind the room. Catch what just happened.",
            pack.type.body,
            c.inkMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        GeckoButton(
            label = if (listening) "Listening — tap to pause" else "Tap to start listening",
            onClick = onToggleListen,
            tone = if (listening) GeckoButtonTone.Accent else GeckoButtonTone.Ink,
            leadingIcon = if (listening) R.drawable.ic_ph_microphone else R.drawable.ic_ph_microphone_slash,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        MemoryDial(
            memorizedSeconds = state.memorizedSeconds,
            totalMemorySeconds = state.totalMemorySeconds,
            mode = state.mode,
        )
        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(visible = recording, enter = fadeIn(), exit = fadeOut()) {
            PackSurface(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GeckoIcon(R.drawable.ic_ph_waveform, null, c.danger, size = 20.dp)
                        GeckoText(
                            "  Live · ${TimeFormat.natural(state.recordedSeconds)}",
                            pack.type.body,
                            c.danger,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GeckoButton(
                        label = "Stop & save",
                        onClick = onStopRecording,
                        tone = GeckoButtonTone.Danger,
                        leadingIcon = R.drawable.ic_ph_stop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        AnimatedVisibility(visible = listening && !recording, enter = fadeIn(), exit = fadeOut()) {
            Column {
                PackSurface(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GeckoIcon(R.drawable.ic_ph_clock, null, c.accent, size = 18.dp)
                            GeckoText("  Memory holds", pack.type.caption, c.inkMuted)
                        }
                        GeckoText(
                            TimeFormat.natural(state.memorizedSeconds),
                            pack.type.headline,
                            c.ink,
                            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                        )
                        GeckoText(
                            "Tap a span to save. Press & hold to keep recording after.",
                            pack.type.caption,
                            c.inkMuted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                GeckoChipRow(
                    enabled = !state.busy,
                    memorizedLabel = TimeFormat.shortTimer(state.memorizedSeconds),
                    onSave = { pendingSeconds = it },
                    onSaveAndContinue = onSaveAndContinue,
                )
                GeckoText(
                    "Buffer capped at ${TimeFormat.natural(state.totalMemorySeconds)} · ${pack.displayName} pack",
                    pack.type.caption,
                    c.surfaceBorder,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        AnimatedVisibility(visible = !listening && !recording) {
            PackSurface(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                GeckoText(
                    "When listening is on, Gecko keeps a rolling audio buffer so you can save the past.",
                    pack.type.body,
                    c.inkMuted,
                )
            }
        }
    }

    if (state.busy) {
        GeckoBusyOverlay("Writing WAV to Music/Gecko…")
    }

    pendingSeconds?.let { seconds ->
        GeckoDialog(
            onDismiss = { pendingSeconds = null },
            title = "Name this capture",
            body = {
                GeckoField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    placeholder = "Gecko moment",
                )
            },
            confirmLabel = "Save",
            onConfirm = {
                onSave(seconds, nameDraft.trim().ifEmpty { null })
                pendingSeconds = null
                nameDraft = ""
            },
        )
    }

    state.lastSavedFile?.let { file ->
        GeckoDialog(
            onDismiss = onDismissSaved,
            title = "Ready",
            body = {
                Column {
                    GeckoText(file.name, pack.type.body, c.ink)
                    GeckoText(
                        "Length ${TimeFormat.natural(state.lastSavedRuntime)} · ${TimeFormat.fileSize(file.length())}",
                        pack.type.caption,
                        c.inkMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    GeckoText(
                        "Also under Music/Gecko",
                        pack.type.caption,
                        c.surfaceBorder,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            confirmLabel = "Open",
            onConfirm = { onOpenFile(file) },
            dismissLabel = "Done",
            secondaryLabel = "Share",
            onSecondary = { onShareFile(file) },
        )
    }
}
