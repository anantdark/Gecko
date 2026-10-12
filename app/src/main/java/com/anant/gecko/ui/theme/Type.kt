package com.anant.gecko.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.anant.gecko.R

/** Shared typefaces; each pack composes its own [PackType] styles from these. */
val DisplayFamily = FontFamily(
    Font(R.font.syne_variable, weight = FontWeight.Bold),
    Font(R.font.syne_variable, weight = FontWeight.ExtraBold),
)

val BodyFamily = FontFamily(
    Font(R.font.figtree_variable, weight = FontWeight.Normal),
    Font(R.font.figtree_variable, weight = FontWeight.Medium),
    Font(R.font.figtree_variable, weight = FontWeight.SemiBold),
)

val MonoFamily = FontFamily(
    Font(R.font.ibm_plex_mono_medium, weight = FontWeight.Medium),
    Font(R.font.ibm_plex_mono_bold, weight = FontWeight.Bold),
)
