plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")

    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.5")
}
