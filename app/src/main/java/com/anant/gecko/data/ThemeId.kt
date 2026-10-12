package com.anant.gecko.data

enum class ThemeId(val storageKey: String) {
    Biolume("biolume"),
    Sonar("sonar"),
    Oxide("oxide"),
    Volt("volt");

    companion object {
        fun fromKey(key: String?): ThemeId =
            entries.firstOrNull { it.storageKey == key } ?: Biolume
    }
}
