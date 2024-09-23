plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.xpdustry.com/releases") {
        name = "xpdustry-releases"
        mavenContent { releasesOnly() }
    }
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.2")
    implementation("com.xpdustry.ksr:com.xpdustry.ksr.gradle.plugin:1.0.0") {
        exclude(group = "com.github.johnrengelman")
    }

    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
}
