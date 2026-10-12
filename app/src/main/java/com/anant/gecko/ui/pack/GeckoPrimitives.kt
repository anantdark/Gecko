package com.anant.gecko.ui.pack

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.gecko.ui.theme.LocalGeckoTheme
import com.anant.gecko.ui.theme.SurfaceStyle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun GeckoText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        modifier = modifier,
    )
}

@Composable
fun PackSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val surfaceMod = when (pack.surfaceStyle) {
        SurfaceStyle.SoftLeaf -> Modifier
            .background(c.surface)
            .border(pack.shapes.hairline, c.surfaceBorder.copy(alpha = 0.45f), pack.shapes.panel)
        SurfaceStyle.GlassNight -> Modifier
            .background(c.surface.copy(alpha = 0.72f))
            .border(pack.shapes.hairline, c.accent.copy(alpha = 0.35f), pack.shapes.panel)
        SurfaceStyle.EmbossedMetal -> Modifier
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(c.surface.copy(alpha = 0.95f), c.canvasDeep),
                        start = Offset.Zero,
                        end = Offset(0f, size.height),
                    ),
                )
                drawLine(
                    c.ink.copy(alpha = 0.12f),
                    Offset(0f, 1f),
                    Offset(size.width, 1f),
                    strokeWidth = 2f,
                )
            }
            .border(pack.shapes.hairline, c.surfaceBorder, pack.shapes.panel)
        SurfaceStyle.NeonFlat -> Modifier
            .background(c.surface)
            .border(pack.shapes.hairline, c.accent, pack.shapes.panel)
    }
    Box(
        modifier = modifier
            .clip(pack.shapes.panel)
            .then(surfaceMod)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun GeckoIcon(
    resId: Int,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val painter: Painter = rememberVectorPainter(ImageVector.vectorResource(resId))
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun GeckoIconButton(
    resId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalGeckoTheme.current.colors.inkMuted,
) {
    val pack = LocalGeckoTheme.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(pack.shapes.chip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GeckoIcon(resId, contentDescription, tint)
    }
}

enum class GeckoButtonTone { Accent, Danger, Ghost, Ink }

@Composable
fun GeckoButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GeckoButtonTone = GeckoButtonTone.Accent,
    enabled: Boolean = true,
    leadingIcon: Int? = null,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pack.motion.pressScale else 1f,
        label = "btnScale",
    )
    val bg = when (tone) {
        GeckoButtonTone.Accent -> c.accent
        GeckoButtonTone.Danger -> c.danger
        GeckoButtonTone.Ghost -> Color.Transparent
        GeckoButtonTone.Ink -> c.ink.copy(alpha = 0.55f)
    }
    val fg = when (tone) {
        GeckoButtonTone.Accent -> c.onAccent
        GeckoButtonTone.Danger -> c.onDanger
        GeckoButtonTone.Ghost -> c.ink
        GeckoButtonTone.Ink -> c.canvas
    }
    Row(
        modifier = modifier
            .scale(scale)
            .height(54.dp)
            .clip(pack.shapes.pill)
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .then(
                if (tone == GeckoButtonTone.Ghost) {
                    Modifier.border(pack.shapes.hairline, c.surfaceBorder, pack.shapes.pill)
                } else Modifier
            )
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            GeckoIcon(leadingIcon, null, fg, size = 18.dp)
            Spacer(Modifier.width(10.dp))
        }
        GeckoText(
            text = label,
            style = pack.type.caption,
            color = fg,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun GeckoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val bg = when {
        !enabled -> c.surface.copy(alpha = 0.4f)
        selected -> c.accent
        else -> c.surface
    }
    val fg = when {
        selected -> c.onAccent
        else -> c.ink
    }
    val border = when {
        selected -> c.accent
        else -> c.surfaceBorder.copy(alpha = 0.5f)
    }
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(pack.shapes.chip)
            .background(bg)
            .border(pack.shapes.hairline, border, pack.shapes.chip)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        enabled = enabled,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                } else {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        GeckoText(
            text = label,
            style = pack.type.chip,
            color = fg,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun GeckoChipRow(
    enabled: Boolean,
    memorizedLabel: String,
    onSave: (Float) -> Unit,
    onSaveAndContinue: (Float) -> Unit,
) {
    val rows = listOf(
        listOf(
            "1:00" to 60f,
            "5:00" to 300f,
            memorizedLabel.ifBlank { "max" } to Float.MAX_VALUE,
        ),
        listOf(
            "30:00" to 1800f,
            "2 hrs" to 7200f,
            "6 hrs" to 21600f,
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { chips ->
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.spacedBy(10.dp),
                Alignment.CenterVertically,
            ) {
                chips.forEach { (label, seconds) ->
                    GeckoChip(
                        label = label,
                        selected = false,
                        enabled = enabled,
                        onClick = { onSave(seconds) },
                        onLongClick = { onSaveAndContinue(seconds) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun GeckoField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = pack.type.body.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier
            .fillMaxWidth()
            .clip(pack.shapes.chip)
            .background(c.canvasDeep.copy(alpha = 0.35f))
            .border(pack.shapes.hairline, c.surfaceBorder, pack.shapes.chip)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                GeckoText(
                    text = placeholder,
                    style = pack.type.body,
                    color = c.inkMuted,
                )
            }
            inner()
        },
    )
}

@Composable
fun GeckoDialog(
    onDismiss: () -> Unit,
    title: String,
    body: @Composable () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = "Cancel",
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .clip(pack.shapes.dialog)
                .background(c.surface)
                .border(pack.shapes.hairline, c.surfaceBorder, pack.shapes.dialog)
                .padding(20.dp),
        ) {
            GeckoText(text = title, style = pack.type.headline, color = c.ink)
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 18.dp)) {
                body()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                if (dismissLabel != null) {
                    GeckoButton(
                        label = dismissLabel,
                        onClick = onDismiss,
                        tone = GeckoButtonTone.Ghost,
                    )
                }
                if (secondaryLabel != null && onSecondary != null) {
                    GeckoButton(
                        label = secondaryLabel,
                        onClick = onSecondary,
                        tone = GeckoButtonTone.Ghost,
                    )
                }
                GeckoButton(
                    label = confirmLabel,
                    onClick = onConfirm,
                    tone = GeckoButtonTone.Accent,
                )
            }
        }
    }
}

@Composable
fun ThemeSwatch(
    packPreview: com.anant.gecko.ui.theme.GeckoThemePack,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = LocalGeckoTheme.current
    Column(
        modifier = modifier
            .clip(host.shapes.chip)
            .border(
                host.shapes.hairline,
                if (selected) host.colors.accent else host.colors.surfaceBorder.copy(alpha = 0.4f),
                host.shapes.chip,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                packPreview.colors.canvas,
                packPreview.colors.accent,
                packPreview.colors.danger,
                packPreview.colors.ink,
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(host.shapes.pill)
                        .background(color)
                        .border(1.dp, host.colors.surfaceBorder.copy(alpha = 0.3f), host.shapes.pill),
                )
            }
        }
        GeckoText(
            packPreview.displayName,
            host.type.caption,
            host.colors.ink,
            modifier = Modifier.padding(top = 10.dp),
        )
        GeckoText(
            packPreview.tagline,
            host.type.caption.copy(fontSize = host.type.caption.fontSize * 0.9f),
            host.colors.inkMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
