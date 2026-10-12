package com.anant.gecko.ui.pack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.anant.gecko.ui.theme.LocalGeckoTheme

@Composable
fun GeckoBusyOverlay(message: String) {
    val pack = LocalGeckoTheme.current
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .clip(pack.shapes.dialog)
                .background(pack.colors.surface)
                .padding(horizontal = 28.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            GeckoText(message, pack.type.body, pack.colors.ink)
        }
    }
}
