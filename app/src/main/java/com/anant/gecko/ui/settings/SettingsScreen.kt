package com.anant.gecko.ui.settings

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
import com.anant.gecko.data.GeckoPreferences
import com.anant.gecko.data.IconMorph
import com.anant.gecko.data.ThemeId
import com.anant.gecko.service.GeckoRecordingService
import com.anant.gecko.ui.pack.GeckoChip
import com.anant.gecko.ui.pack.GeckoDialog
import com.anant.gecko.ui.pack.GeckoIconButton
import com.anant.gecko.ui.pack.GeckoText
import com.anant.gecko.ui.pack.PackSurface
import com.anant.gecko.ui.pack.ThemeSwatch
import com.anant.gecko.ui.theme.GeckoThemeRegistry
import com.anant.gecko.ui.theme.LocalGeckoTheme
import com.anant.gecko.util.TimeFormat

private sealed class PendingCaptureChange {
    data class Memory(val bytes: Long, val label: String) : PendingCaptureChange()
    data class Quality(val rate: Int, val label: String) : PendingCaptureChange()
}

@Composable
fun SettingsScreen(
    memoryBytes: Long,
    sampleRate: Int,
    totalMemorySeconds: Float,
    captureMode: GeckoRecordingService.Mode,
    themeId: ThemeId,
    iconMorph: IconMorph,
    onBack: () -> Unit,
    onMemorySelected: (Long) -> Unit,
    onQualitySelected: (Int) -> Unit,
    onThemeSelected: (ThemeId) -> Unit,
    onIconMorphSelected: (IconMorph) -> Unit,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val max = Runtime.getRuntime().maxMemory()
    val low = max / 4
    val mid = max / 2
    val high = (max * 0.90).toLong()
    val captureActive = captureMode != GeckoRecordingService.Mode.Idle
    val isRecording = captureMode == GeckoRecordingService.Mode.Recording

    var pending by remember { mutableStateOf<PendingCaptureChange?>(null) }
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    fun nearestBucket(bytes: Long): Int = when {
        bytes >= high * 0.85 -> 3
        bytes >= mid * 0.85 -> 2
        else -> 1
    }

    val memorySel = nearestBucket(memoryBytes)
    val qualitySel = when {
        sampleRate >= 44_100 -> 3
        sampleRate >= 16_000 -> 2
        else -> 1
    }

    fun requestMemory(bytes: Long, bucket: Int) {
        if (bucket == memorySel) return
        val label = TimeFormat.fileSize(bytes)
        if (!captureActive) {
            onMemorySelected(bytes)
            return
        }
        pending = PendingCaptureChange.Memory(bytes, label)
    }

    fun requestQuality(rate: Int, index: Int) {
        if (index + 1 == qualitySel) return
        val label = when (rate) {
            8_000 -> "8 kHz"
            16_000 -> "16 kHz"
            else -> "44.1 kHz"
        }
        if (isRecording) {
            blockedMessage = "Stop the live recording before changing sound quality."
            return
        }
        if (!captureActive) {
            onQualitySelected(rate)
            return
        }
        pending = PendingCaptureChange.Quality(rate, label)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 36.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GeckoIconButton(
                resId = R.drawable.ic_ph_caret_left,
                contentDescription = "Back",
                onClick = onBack,
                tint = c.ink,
            )
            GeckoText("Settings", pack.type.headline, c.ink)
        }

        Spacer(Modifier.height(20.dp))

        GeckoText("UI pack", pack.type.headline, c.ink)
        GeckoText(
            "Each pack swaps colors, shapes, dial, and motion — not Material skins.",
            pack.type.caption,
            c.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GeckoThemeRegistry.all.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { preview ->
                        ThemeSwatch(
                            packPreview = preview,
                            selected = preview.id == themeId,
                            onClick = { onThemeSelected(preview.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        GeckoText("Launcher chameleon", pack.type.headline, c.ink)
        GeckoText(
            "Pick a green tint for the home icon. Changing it swaps the launcher entry — if a shortcut says “not installed”, open Gecko once from the app drawer and the icon will refresh.",
            pack.type.caption,
            c.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        IconMorph.entries.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                row.forEach { morph ->
                    GeckoChip(
                        label = morph.label,
                        selected = iconMorph == morph,
                        onClick = { onIconMorphSelected(morph) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(16.dp))

        PackSurface(modifier = Modifier.fillMaxWidth()) {
            Column {
                GeckoText(
                    "Audio memory limited at ${TimeFormat.natural(totalMemorySeconds)}",
                    pack.type.body,
                    c.ink,
                )
                GeckoText(
                    "Lower RAM and sample rate = longer history and quieter battery.",
                    pack.type.caption,
                    c.inkMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        GeckoText("A) System memory in use", pack.type.headline, c.ink)
        GeckoText(
            "Bigger buffers hold more past — and may nudge other apps.",
            pack.type.caption,
            c.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GeckoChip(
                label = TimeFormat.fileSize(low),
                selected = memorySel == 1,
                onClick = { requestMemory(low, 1) },
                modifier = Modifier.weight(1f),
            )
            GeckoChip(
                label = TimeFormat.fileSize(mid),
                selected = memorySel == 2,
                onClick = { requestMemory(mid, 2) },
                modifier = Modifier.weight(1f),
            )
            GeckoChip(
                label = TimeFormat.fileSize(high),
                selected = memorySel == 3,
                onClick = { requestMemory(high, 3) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(28.dp))
        GeckoText("B) Sound quality", pack.type.headline, c.ink)
        GeckoText(
            "8 kHz (default) is best for battery and still clear for speech. 16 kHz is a balance. 44.1 kHz costs ~5× the PCM of 8 kHz.",
            pack.type.caption,
            c.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GeckoPreferences.QUALITY_OPTIONS.forEachIndexed { index, rate ->
                GeckoChip(
                    label = when (rate) {
                        8_000 -> "8 kHz"
                        16_000 -> "16 kHz"
                        else -> "44.1"
                    },
                    selected = qualitySel == index + 1,
                    onClick = { requestQuality(rate, index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        GeckoButtonBack(onBack)
    }

    pending?.let { change ->
        val (title, body) = when (change) {
            is PendingCaptureChange.Memory ->
                "Resize audio memory?" to
                    "Switch to ${change.label}? Gecko will rebuild the listening buffer and clear what’s currently held in memory."
            is PendingCaptureChange.Quality ->
                "Change sound quality?" to
                    "Switch to ${change.label}? Capture restarts briefly — the rolling buffer is cleared."
        }
        GeckoDialog(
            onDismiss = { pending = null },
            title = title,
            body = {
                GeckoText(body, pack.type.body, c.inkMuted)
            },
            confirmLabel = "Save change",
            onConfirm = {
                when (val cng = change) {
                    is PendingCaptureChange.Memory -> onMemorySelected(cng.bytes)
                    is PendingCaptureChange.Quality -> onQualitySelected(cng.rate)
                }
                pending = null
            },
            dismissLabel = "Cancel",
        )
    }

    blockedMessage?.let { message ->
        GeckoDialog(
            onDismiss = { blockedMessage = null },
            title = "Recording in progress",
            body = {
                GeckoText(message, pack.type.body, c.inkMuted)
            },
            confirmLabel = "OK",
            onConfirm = { blockedMessage = null },
            dismissLabel = null,
        )
    }
}

@Composable
private fun GeckoButtonBack(onBack: () -> Unit) {
    com.anant.gecko.ui.pack.GeckoButton(
        label = "Return",
        onClick = onBack,
        tone = com.anant.gecko.ui.pack.GeckoButtonTone.Ghost,
        modifier = Modifier.fillMaxWidth(),
    )
}
