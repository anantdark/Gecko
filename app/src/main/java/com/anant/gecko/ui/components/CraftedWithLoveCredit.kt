package com.anant.gecko.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anant.gecko.R
import com.anant.gecko.ui.pack.GeckoIcon
import com.anant.gecko.ui.pack.GeckoText
import com.anant.gecko.ui.theme.LocalGeckoTheme

private const val ANANT_SITE_URL = "https://anantdark.github.io"

/** Three-line credit with a double-beat heart. */
@Composable
fun CraftedWithLoveCredit(
    modifier: Modifier = Modifier,
    onHeartDoubleTap: () -> Unit = {},
) {
    val pack = LocalGeckoTheme.current
    val c = pack.colors
    val beat = rememberInfiniteTransition(label = "craftedHeart")
    val scale by beat.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.00f at 0 using FastOutSlowInEasing
                1.28f at 140 using FastOutSlowInEasing
                1.00f at 280 using FastOutSlowInEasing
                1.18f at 400 using FastOutSlowInEasing
                1.00f at 540 using FastOutSlowInEasing
                1.00f at 1200
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "heartScale",
    )
    val hue by beat.animateFloat(
        initialValue = 335f,
        targetValue = 335f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                335f at 0 using FastOutSlowInEasing
                358f at 140 using FastOutSlowInEasing
                348f at 280 using FastOutSlowInEasing
                368f at 400 using FastOutSlowInEasing
                342f at 540 using FastOutSlowInEasing
                335f at 1200
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "heartHue",
    )
    val sat by beat.animateFloat(
        initialValue = 0.62f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.62f at 0 using FastOutSlowInEasing
                0.82f at 140 using FastOutSlowInEasing
                0.68f at 280 using FastOutSlowInEasing
                0.88f at 400 using FastOutSlowInEasing
                0.70f at 540 using FastOutSlowInEasing
                0.62f at 1200
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "heartSat",
    )
    val heartColor = Color.hsv(
        hue = ((hue % 360f) + 360f) % 360f,
        saturation = sat,
        value = 0.95f,
    )
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GeckoText("Crafted with", pack.type.headline, c.inkMuted)
        GeckoIcon(
            resId = R.drawable.ic_ph_heart,
            contentDescription = "Double-tap to celebrate",
            tint = heartColor,
            size = 72.dp,
            modifier = Modifier
                .padding(vertical = 14.dp)
                .size(72.dp)
                .scale(scale)
                .semantics { role = Role.Button }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onHeartDoubleTap() })
                },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            GeckoText("by ", pack.type.headline, c.ink)
            GeckoText(
                text = "Anant",
                style = pack.type.headline,
                color = c.accent,
                modifier = Modifier.clickable(role = Role.Button) {
                    uriHandler.openUri(ANANT_SITE_URL)
                },
            )
        }
    }
}
