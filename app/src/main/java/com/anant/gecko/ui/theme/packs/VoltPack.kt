package com.anant.gecko.ui.theme.packs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.gecko.ui.theme.BodyFamily
import com.anant.gecko.ui.theme.DialStyle
import com.anant.gecko.ui.theme.DisplayFamily
import com.anant.gecko.ui.theme.GeckoThemePack
import com.anant.gecko.ui.theme.MonoFamily
import com.anant.gecko.ui.theme.PackColors
import com.anant.gecko.ui.theme.PackMotion
import com.anant.gecko.ui.theme.PackShapes
import com.anant.gecko.ui.theme.PackType
import com.anant.gecko.ui.theme.SurfaceStyle
import com.anant.gecko.data.ThemeId

/** Signal board — ink void, acid chartreuse voltage. */
val VoltPack = GeckoThemePack(
    id = ThemeId.Volt,
    displayName = "Volt",
    tagline = "Hard edges. Loud signal. Zero fluff.",
    colors = PackColors(
        canvas = Color(0xFF0A0A0A),
        canvasDeep = Color(0xFF141414),
        ink = Color(0xFFF4F4F0),
        inkMuted = Color(0xFF8A8A82),
        accent = Color(0xFFD6FF3A),
        accentGlow = Color(0xFFEFFF8A),
        danger = Color(0xFFFF3D5A),
        dangerSoft = Color(0xFFFF8A9A),
        surface = Color(0xFF161616),
        surfaceBorder = Color(0xFF2E2E2A),
        dialVoid = Color(0xFF000000),
        onAccent = Color(0xFF0A0A0A),
        onDanger = Color(0xFF0A0A0A),
        glow = Color(0xFFD6FF3A),
    ),
    shapes = PackShapes(
        pill = RoundedCornerShape(0.dp),
        panel = RoundedCornerShape(0.dp),
        chip = RoundedCornerShape(0.dp),
        dialog = RoundedCornerShape(0.dp),
        hairline = 2.dp,
    ),
    type = PackType(
        brand = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, letterSpacing = (-2).sp),
        headline = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.5).sp),
        body = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
        caption = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
        timer = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-1).sp),
        chip = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    ),
    dialStyle = DialStyle.SpectrumBars,
    surfaceStyle = SurfaceStyle.NeonFlat,
    motion = PackMotion(pulseMs = 900, sweepMs = 4000, pressScale = 0.96f),
)
