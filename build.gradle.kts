// Root build configuration
plugins {
    // Declare plugins without applying them
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqlDelight) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
