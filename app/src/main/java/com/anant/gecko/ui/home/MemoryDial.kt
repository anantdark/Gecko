package com.anant.gecko.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anant.gecko.service.GeckoRecordingService
import com.anant.gecko.ui.pack.GeckoText
import com.anant.gecko.ui.theme.DialStyle
import com.anant.gecko.ui.theme.LocalGeckoTheme
import com.anant.gecko.util.TimeFormat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MemoryDial(
    memorizedSeconds: Float,
    totalMemorySeconds: Float,
    mode: GeckoRecordingService.Mode,
    modifier: Modifier = Modifier,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val fill = if (totalMemorySeconds > 0f) {
        (memorizedSeconds / totalMemorySeconds).coerceIn(0f, 1f)
    } else {
        0f
    }
    val active = mode != GeckoRecordingService.Mode.Idle
    val recording = mode == GeckoRecordingService.Mode.Recording
    val accent = when {
        recording -> c.danger
        active -> c.accent
        else -> c.surfaceBorder
    }

    val pulse by rememberInfiniteTransition(label = "dial").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(pack.motion.pulseMs, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val sweep by rememberInfiniteTransition(label = "sweep").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(pack.motion.sweepMs, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(260.dp)) {
            when (pack.dialStyle) {
                DialStyle.ArcGlow -> drawArcGlow(fill, accent, c.dialVoid, c.surfaceBorder, c.glow, active, pulse, sweep)
                DialStyle.RadarSweep -> drawRadar(fill, accent, c.dialVoid, c.surfaceBorder, active, sweep)
                DialStyle.TapeReel -> drawTape(fill, accent, c.dialVoid, c.surfaceBorder, c.accentGlow, active, sweep)
                DialStyle.SpectrumBars -> drawSpectrum(fill, accent, c.dialVoid, c.surfaceBorder, active, pulse)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp),
        ) {
            GeckoText(
                text = if (active) TimeFormat.shortTimer(memorizedSeconds) else "—:—",
                style = pack.type.timer,
                color = if (recording) c.dangerSoft else c.accentGlow,
                textAlign = TextAlign.Center,
            )
            GeckoText(
                text = if (active) "of ${TimeFormat.shortTimer(totalMemorySeconds)} held" else "listening off",
                style = pack.type.caption,
                // Dial void is always dark — keep caption light across packs
                color = c.accentGlow.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcGlow(
    fill: Float,
    accent: Color,
    void: Color,
    track: Color,
    glow: Color,
    active: Boolean,
    pulse: Float,
    sweep: Float,
) {
    val stroke = 18.dp.toPx()
    val diameter = min(size.width, size.height) - stroke
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    drawCircle(
        brush = Brush.radialGradient(listOf(void, void.copy(alpha = 0.85f))),
        radius = diameter / 2f - stroke * 0.35f,
    )
    drawArc(
        color = track.copy(alpha = 0.35f),
        startAngle = -210f,
        sweepAngle = 240f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawArc(
        brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.4f), accent, glow)),
        startAngle = -210f,
        sweepAngle = 240f * fill,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    if (active) {
        drawCircle(color = accent.copy(alpha = 0.12f * pulse), radius = (diameter / 2f) * pulse)
        rotate(sweep) {
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 3.dp.toPx(),
                center = Offset(size.width / 2f, topLeft.y + stroke * 0.2f),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadar(
    fill: Float,
    accent: Color,
    void: Color,
    track: Color,
    active: Boolean,
    sweep: Float,
) {
    val r = min(size.width, size.height) / 2f * 0.92f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(void, r)
    for (i in 1..4) {
        drawCircle(
            color = track.copy(alpha = 0.35f),
            radius = r * i / 4f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
    // crosshairs
    drawLine(track.copy(alpha = 0.4f), Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1.dp.toPx())
    drawLine(track.copy(alpha = 0.4f), Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1.dp.toPx())
    // fill wedge
    drawArc(
        color = accent.copy(alpha = 0.22f),
        startAngle = -90f,
        sweepAngle = 360f * fill,
        useCenter = true,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2, r * 2),
    )
    if (active) {
        rotate(sweep, center) {
            drawLine(
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0f), accent),
                    start = center,
                    end = Offset(center.x, center.y - r),
                ),
                start = center,
                end = Offset(center.x, center.y - r),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // blip on fill edge
        val ang = Math.toRadians((-90f + 360f * fill).toDouble())
        val blip = Offset(
            center.x + (r * fill.coerceAtLeast(0.15f) * cos(ang)).toFloat(),
            center.y + (r * fill.coerceAtLeast(0.15f) * sin(ang)).toFloat(),
        )
        drawCircle(accent, 4.dp.toPx(), blip)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTape(
    fill: Float,
    accent: Color,
    void: Color,
    track: Color,
    glow: Color,
    active: Boolean,
    sweep: Float,
) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = void,
        topLeft = Offset(w * 0.08f, h * 0.18f),
        size = Size(w * 0.84f, h * 0.64f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
    )
    val reelR = h * 0.18f
    val left = Offset(w * 0.30f, h * 0.48f)
    val right = Offset(w * 0.70f, h * 0.48f)
    fun reel(center: Offset, spin: Float) {
        drawCircle(track.copy(alpha = 0.5f), reelR, center, style = Stroke(5.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.25f + fill * 0.5f), reelR * (0.35f + fill * 0.45f), center)
        rotate(if (active) spin else 0f, center) {
            for (i in 0 until 6) {
                val a = Math.toRadians(i * 60.0)
                drawLine(
                    glow.copy(alpha = 0.7f),
                    center,
                    Offset(center.x + (reelR * 0.85f * cos(a)).toFloat(), center.y + (reelR * 0.85f * sin(a)).toFloat()),
                    2.dp.toPx(),
                )
            }
        }
    }
    reel(left, sweep)
    reel(right, -sweep * 1.2f)
    // tape bridge
    drawLine(accent, Offset(left.x + reelR * 0.6f, left.y - reelR * 0.7f), Offset(right.x - reelR * 0.6f, right.y - reelR * 0.7f), 3.dp.toPx())
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpectrum(
    fill: Float,
    accent: Color,
    void: Color,
    track: Color,
    active: Boolean,
    pulse: Float,
) {
    drawRect(void)
    val bars = 16
    val gap = 4.dp.toPx()
    val barW = (size.width - gap * (bars + 1)) / bars
    for (i in 0 until bars) {
        val t = i / (bars - 1f)
        val base = 0.15f + 0.85f * fill * (0.4f + 0.6f * sin((t * 3.1f + pulse) * Math.PI).toFloat().coerceIn(0f, 1f))
        val h = size.height * if (active) base else 0.12f
        val x = gap + i * (barW + gap)
        val y = (size.height - h) / 2f
        drawRect(
            color = if (t <= fill) accent else track.copy(alpha = 0.35f),
            topLeft = Offset(x, y),
            size = Size(barW, h),
        )
    }
}
