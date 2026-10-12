package com.anant.gecko.data

/**
 * Pre-rendered launcher chameleon colors (Wikimedia CC0 base, green-adjacent morphs).
 * Android cannot arbitrarily tint the home-screen icon — we switch among
 * activity-alias icons.
 */
enum class IconMorph(val storageKey: String, val label: String, val aliasSuffix: String) {
    Green("green", "Green", "AliasGreen"),
    Mint("mint", "Mint", "AliasMint"),
    Sage("sage", "Sage", "AliasSage"),
    Olive("olive", "Olive", "AliasOlive"),
    Forest("forest", "Forest", "AliasForest"),
    Seafoam("seafoam", "Seafoam", "AliasSeafoam"),
    ;

    companion object {
        fun fromKey(key: String?): IconMorph =
            entries.firstOrNull { it.storageKey == key } ?: Green
    }
}

enum class IconMorphSchedule(val storageKey: String, val label: String) {
    Fixed("fixed", "Fixed"),
    EveryLaunch("every_launch", "Every launch"),
    Daily("daily", "Every day"),
    ;

    companion object {
        fun fromKey(key: String?): IconMorphSchedule =
            entries.firstOrNull { it.storageKey == key } ?: Fixed
    }
}
