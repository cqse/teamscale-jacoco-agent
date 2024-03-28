import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("io.github.goooler.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    isEnableRelocation = true
    // Needed as a workaround for https://github.com/johnrengelman/shadow/issues/521
    inputs.property("debug", project.hasProperty("debug"))
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
    manifest {
        // The jaxb library, which we are shading is a multi release jar, so we have to explicitly "inherit" this attribute
        // https://github.com/johnrengelman/shadow/issues/449
        attributes["Multi-Release"] = "true"
    }
}

// Defer the resolution of 'runtimeClasspath'. This is an issue in the shadow
// plugin that it automatically accesses the files in 'runtimeClasspath' while
// Gradle is building the task graph. The lines below work around that.
// https://github.com/johnrengelman/shadow/issues/882
tasks.withType<ShadowJar> {
    dependsOn(tasks.jar)
    inputs.files(project.configurations.runtimeClasspath)
    configurations = emptyList()
    doFirst { configurations = listOf(project.configurations.runtimeClasspath.get()) }
}
