package com.anant.gecko.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anant.gecko.data.ThemeId
import com.anant.gecko.ui.theme.packs.BiolumePack
import com.anant.gecko.ui.theme.packs.OxidePack
import com.anant.gecko.ui.theme.packs.SonarPack
import com.anant.gecko.ui.theme.packs.VoltPack

enum class DialStyle {
    /** Soft arc + breathing halo (Biolume). */
    ArcGlow,
    /** Full radar rings + sweep (Sonar). */
    RadarSweep,
    /** Twin reels / tape counter (Oxide). */
    TapeReel,
    /** Vertical spectrum bars (Volt). */
    SpectrumBars,
}

enum class SurfaceStyle {
    SoftLeaf,
    GlassNight,
    EmbossedMetal,
    NeonFlat,
}

@Immutable
data class PackColors(
    val canvas: Color,
    val canvasDeep: Color,
    val ink: Color,
    val inkMuted: Color,
    val accent: Color,
    val accentGlow: Color,
    val danger: Color,
    val dangerSoft: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val dialVoid: Color,
    val onAccent: Color,
    val onDanger: Color,
    val glow: Color,
)

@Immutable
data class PackShapes(
    val pill: Shape,
    val panel: Shape,
    val chip: Shape,
    val dialog: Shape,
    val hairline: Dp = 1.5.dp,
)

@Immutable
data class PackType(
    val brand: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val timer: TextStyle,
    val chip: TextStyle,
)

@Immutable
data class PackMotion(
    val pulseMs: Int = 1600,
    val sweepMs: Int = 9000,
    val pressScale: Float = 0.97f,
)

@Immutable
data class GeckoThemePack(
    val id: ThemeId,
    val displayName: String,
    val tagline: String,
    val colors: PackColors,
    val shapes: PackShapes,
    val type: PackType,
    val dialStyle: DialStyle,
    val surfaceStyle: SurfaceStyle,
    val motion: PackMotion = PackMotion(),
)

val LocalGeckoTheme = staticCompositionLocalOf { BiolumePack }

object GeckoThemeRegistry {
    val all: List<GeckoThemePack> = listOf(
        BiolumePack,
        SonarPack,
        OxidePack,
        VoltPack,
    )

    fun resolve(id: ThemeId): GeckoThemePack =
        all.firstOrNull { it.id == id } ?: BiolumePack
}
