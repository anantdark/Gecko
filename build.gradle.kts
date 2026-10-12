plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

buildscript {
    configurations.classpath {
        resolutionStrategy {
            force("org.jetbrains:annotations:26.1.0")
        }
    }
}
