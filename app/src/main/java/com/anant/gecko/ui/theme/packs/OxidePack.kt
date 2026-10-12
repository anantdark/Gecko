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

/** Field recorder — slate chassis, copper oxide accents. */
val OxidePack = GeckoThemePack(
    id = ThemeId.Oxide,
    displayName = "Oxide",
    tagline = "Copper reels on a worn slate deck",
    colors = PackColors(
        canvas = Color(0xFF2A2C2E),
        canvasDeep = Color(0xFF1C1E20),
        ink = Color(0xFFE8E2D6),
        inkMuted = Color(0xFFA39B8C),
        accent = Color(0xFFC47A45),
        accentGlow = Color(0xFFE8A86A),
        danger = Color(0xFFD64545),
        dangerSoft = Color(0xFFE89A9A),
        surface = Color(0xFF35383B),
        surfaceBorder = Color(0xFF4A4540),
        dialVoid = Color(0xFF121314),
        onAccent = Color(0xFF1A120C),
        onDanger = Color(0xFFFFF5F5),
        glow = Color(0xFFC47A45),
    ),
    shapes = PackShapes(
        pill = RoundedCornerShape(10.dp),
        panel = RoundedCornerShape(14.dp),
        chip = RoundedCornerShape(10.dp),
        dialog = RoundedCornerShape(16.dp),
        hairline = 2.dp,
    ),
    type = PackType(
        brand = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-0.5).sp),
        headline = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        body = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        caption = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp),
        timer = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp),
        chip = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    ),
    dialStyle = DialStyle.TapeReel,
    surfaceStyle = SurfaceStyle.EmbossedMetal,
    motion = PackMotion(pulseMs = 1800, sweepMs = 8000, pressScale = 0.98f),
)
