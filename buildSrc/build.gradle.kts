plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("io.github.goooler.shadow:shadow-gradle-plugin:8.1.8")

    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
}
