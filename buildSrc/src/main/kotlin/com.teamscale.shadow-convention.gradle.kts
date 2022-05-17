import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

tasks.register<ConfigureShadowRelocation>("relocateShadowJar" ) {
    target = tasks.getByName<ShadowJar>( "shadowJar")
    onlyIf {
        !project.hasProperty("debug")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    // Needed as a workaround for https://github.com/johnrengelman/shadow/issues/521
    inputs.property("debug", project.hasProperty("debug"))
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
    dependsOn(tasks.named("relocateShadowJar"))
}
