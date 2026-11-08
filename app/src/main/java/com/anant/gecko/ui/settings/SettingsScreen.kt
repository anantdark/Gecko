package com.anant.gecko.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.anant.gecko.BuildConfig
import com.anant.gecko.R
import com.anant.gecko.data.GeckoPreferences
import com.anant.gecko.data.IconMorph
import com.anant.gecko.data.ThemeId
import com.anant.gecko.service.GeckoRecordingService
import com.anant.gecko.ui.components.CraftedWithLoveCredit
import com.anant.gecko.ui.pack.GeckoChip
import com.anant.gecko.ui.pack.GeckoDialog
import com.anant.gecko.ui.pack.GeckoIcon
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
    val captureActive = captureMode != GeckoRecordingService.Mode.Idle
    val isRecording = captureMode == GeckoRecordingService.Mode.Recording

    var pending by remember { mutableStateOf<PendingCaptureChange?>(null) }
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    // Exact match against the fixed option list so memory/quality never collapse into each other.
    val memorySel = GeckoPreferences.MEMORY_OPTIONS.minByOrNull { option ->
        kotlin.math.abs(option - memoryBytes)
    } ?: GeckoPreferences.DEFAULT_MEMORY_BYTES
    val qualitySel = GeckoPreferences.QUALITY_OPTIONS.minByOrNull { option ->
        kotlin.math.abs(option - sampleRate)
    } ?: GeckoPreferences.DEFAULT_SAMPLE_RATE

    fun requestMemory(bytes: Long) {
        if (bytes == memorySel) return
        val label = TimeFormat.fileSize(bytes)
        if (!captureActive) {
            onMemorySelected(bytes)
            return
        }
        pending = PendingCaptureChange.Memory(bytes, label)
    }

    fun requestQuality(rate: Int) {
        if (rate == qualitySel) return
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

        Spacer(Modifier.height(18.dp))

        SettingsSection(
            title = "Appearance",
            hint = "Each pack swaps colors, shapes, dial, and motion — not Material skins.",
        ) {
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
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection(
            title = "Launcher chameleon",
            hint = "Pick a green tint for the home icon. Changing it swaps the launcher entry — if a shortcut says “not installed”, open Gecko once from the app drawer and the icon will refresh.",
        ) {
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
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection(
            title = "System memory",
            hint = "Bigger buffers hold more past — and may nudge other apps. Currently holds ${TimeFormat.natural(totalMemorySeconds)}.",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GeckoPreferences.MEMORY_OPTIONS.forEach { bytes ->
                    GeckoChip(
                        label = TimeFormat.fileSize(bytes),
                        selected = bytes == memorySel,
                        onClick = { requestMemory(bytes) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection(
            title = "Sound quality",
            hint = "16 kHz (default) is clear for speech. 8 kHz saves battery. 44.1 kHz costs ~2.5× the PCM of 16 kHz. Memory size stays the same when you change this.",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GeckoPreferences.QUALITY_OPTIONS.forEach { rate ->
                    GeckoChip(
                        label = when (rate) {
                            8_000 -> "8 kHz"
                            16_000 -> "16 kHz"
                            else -> "44.1"
                        },
                        selected = rate == qualitySel,
                        onClick = { requestQuality(rate) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        SettingsSection(
            title = "About",
            hint = "Rewind the room. Catch what just happened.\n\nBuilt with Kotlin and Jetpack Compose. Audio stays on your device.",
        ) {
            AboutRow("App", "Gecko")
            AboutRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            AboutRow("Package", BuildConfig.APPLICATION_ID)
            AboutRow("Channel", if (BuildConfig.IS_FDROID) "F-Droid" else "GitHub")
            AboutRow("Created by", "Anant")
            AboutLinkRow(
                label = "GitHub",
                value = "github.com/anantdark/Gecko",
                url = "https://github.com/anantdark/Gecko",
            )
            AboutLinkRow(
                label = "Author",
                value = "anantdark.github.io",
                url = "https://anantdark.github.io",
            )
            if (BuildConfig.IS_FDROID) {
                AboutLinkRow(
                    label = "F-Droid",
                    value = "f-droid.org/packages/com.anant.gecko",
                    url = "https://f-droid.org/packages/com.anant.gecko/",
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        CraftedWithLoveCredit(modifier = Modifier.fillMaxWidth())
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
private fun SettingsSection(
    title: String,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    PackSurface(modifier = Modifier.fillMaxWidth()) {
        Column {
            GeckoText(title, pack.type.headline, c.ink)
            if (hint != null) {
                GeckoText(
                    hint,
                    pack.type.caption,
                    c.inkMuted,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GeckoText(
            label,
            pack.type.caption,
            c.inkMuted,
            modifier = Modifier.weight(1f),
        )
        GeckoText(value, pack.type.body, c.ink)
    }
}

@Composable
private fun AboutLinkRow(label: String, value: String, url: String) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GeckoText(
            label,
            pack.type.caption,
            c.inkMuted,
            modifier = Modifier.weight(1f),
        )
        GeckoIcon(
            resId = R.drawable.ic_ph_arrow_square_out,
            contentDescription = null,
            tint = c.accent,
            size = 14.dp,
        )
        Spacer(Modifier.size(6.dp))
        GeckoText(value, pack.type.body, c.accent)
    }
}
