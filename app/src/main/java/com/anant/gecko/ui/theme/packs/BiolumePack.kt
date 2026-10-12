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

/** Organic mint canopy — soft leaf surfaces, lichen glow. */
val BiolumePack = GeckoThemePack(
    id = ThemeId.Biolume,
    displayName = "Biolume",
    tagline = "Leaf-lit memory under a cool canopy",
    colors = PackColors(
        canvas = Color(0xFFD5EDE3),
        canvasDeep = Color(0xFFB7DCC9),
        ink = Color(0xFF0E2A1F),
        inkMuted = Color(0xFF3A5C4C),
        accent = Color(0xFF1FA97A),
        accentGlow = Color(0xFF5EF0B0),
        danger = Color(0xFFE85D3A),
        dangerSoft = Color(0xFFFFC4B0),
        surface = Color(0xFFEAF6F0),
        surfaceBorder = Color(0xFF7AA892),
        dialVoid = Color(0xFF071611),
        onAccent = Color(0xFF071611),
        onDanger = Color(0xFFFFFFFF),
        glow = Color(0xFF5EF0B0),
    ),
    shapes = PackShapes(
        pill = RoundedCornerShape(percent = 50),
        panel = RoundedCornerShape(28.dp),
        chip = RoundedCornerShape(22.dp),
        dialog = RoundedCornerShape(28.dp),
    ),
    type = PackType(
        brand = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, letterSpacing = (-1.5).sp),
        headline = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
        body = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        caption = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.2.sp, lineHeight = 18.sp),
        timer = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
        chip = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    ),
    dialStyle = DialStyle.ArcGlow,
    surfaceStyle = SurfaceStyle.SoftLeaf,
    motion = PackMotion(pulseMs = 1600, sweepMs = 10000),
)
