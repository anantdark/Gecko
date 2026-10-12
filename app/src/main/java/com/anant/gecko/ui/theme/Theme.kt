package com.anant.gecko.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.anant.gecko.data.ThemeId

@Composable
fun GeckoTheme(
    themeId: ThemeId = ThemeId.Biolume,
    content: @Composable () -> Unit,
) {
    val pack = remember(themeId) { GeckoThemeRegistry.resolve(themeId) }
    CompositionLocalProvider(
        LocalGeckoTheme provides pack,
        LocalIndication provides ripple(color = pack.colors.accent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawPackCanvas(pack) },
        ) {
            content()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPackCanvas(pack: GeckoThemePack) {
    val c = pack.colors
    drawRect(c.canvas)
    when (pack.surfaceStyle) {
        SurfaceStyle.SoftLeaf -> {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(c.glow.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.12f),
                    radius = size.minDimension * 0.85f,
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, c.canvasDeep.copy(alpha = 0.55f)),
                    startY = size.height * 0.45f,
                    endY = size.height,
                ),
            )
        }
        SurfaceStyle.GlassNight -> {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(c.accent.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.minDimension * 0.9f,
                ),
            )
            // subtle grid
            val step = 28f
            var x = 0f
            while (x < size.width) {
                drawLine(c.surfaceBorder.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(c.surfaceBorder.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }
        SurfaceStyle.EmbossedMetal -> {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(c.canvasDeep, c.canvas, c.canvasDeep),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(c.accent.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.2f),
                    radius = size.minDimension * 0.7f,
                ),
            )
        }
        SurfaceStyle.NeonFlat -> {
            drawRect(c.canvas)
            // dashed frame
            drawRect(
                color = c.accent.copy(alpha = 0.35f),
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f)),
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(c.accent.copy(alpha = 0.08f), Color.Transparent, c.danger.copy(alpha = 0.06f)),
                ),
            )
        }
    }
}
