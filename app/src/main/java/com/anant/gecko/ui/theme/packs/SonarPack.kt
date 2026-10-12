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

/** Night ocean console — cyan radar, glass panels. */
val SonarPack = GeckoThemePack(
    id = ThemeId.Sonar,
    displayName = "Sonar",
    tagline = "Sweep the dark water for echoes",
    colors = PackColors(
        canvas = Color(0xFF06121C),
        canvasDeep = Color(0xFF0A1E2E),
        ink = Color(0xFFD7F3FF),
        inkMuted = Color(0xFF7BA8BE),
        accent = Color(0xFF2EE6D6),
        accentGlow = Color(0xFF7CFFF3),
        danger = Color(0xFFFF5C7A),
        dangerSoft = Color(0xFFFFB0BE),
        surface = Color(0xFF0E2433),
        surfaceBorder = Color(0xFF1E4A5C),
        dialVoid = Color(0xFF02080E),
        onAccent = Color(0xFF021018),
        onDanger = Color(0xFF1A0510),
        glow = Color(0xFF2EE6D6),
    ),
    shapes = PackShapes(
        pill = RoundedCornerShape(6.dp),
        panel = RoundedCornerShape(4.dp),
        chip = RoundedCornerShape(4.dp),
        dialog = RoundedCornerShape(8.dp),
        hairline = 1.dp,
    ),
    type = PackType(
        brand = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 42.sp, letterSpacing = 4.sp),
        headline = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.5.sp),
        body = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        caption = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.2.sp),
        timer = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 1.sp),
        chip = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.8.sp),
    ),
    dialStyle = DialStyle.RadarSweep,
    surfaceStyle = SurfaceStyle.GlassNight,
    motion = PackMotion(pulseMs = 2200, sweepMs = 6000),
)
