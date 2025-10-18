plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
}

dependencies {
    // Provide the coordinates for your Gradle plugins here, including their versions
    implementation("io.freefair.gradle:lombok-plugin:8.6")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
}