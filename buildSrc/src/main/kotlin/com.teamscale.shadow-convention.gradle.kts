import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.xpdustry.ksr.kotlinRelocate

plugins {
	java
	// https://github.com/GradleUp/shadow
	id("com.gradleup.shadow")
	// https://github.com/xpdustry/kotlin-shadow-relocator
	id("com.xpdustry.kotlin-shadow-relocator")
}

tasks.named<ShadowJar>("shadowJar") {
	isEnableRelocation = project.properties["debug"] !== "true"
	// Needed as a workaround for https://github.com/GradleUp/shadow/issues/521
	inputs.property("relocation-enabled", isEnableRelocation)
	archiveClassifier.set(null as String?)
	mergeServiceFiles()
	manifest {
		// The jaxb library, which we are shading is a multi release jar, so we have to explicitly "inherit" this attribute
		// https://github.com/GradleUp/shadow/issues/449
		attributes["Multi-Release"] = "true"
	}
	// Relocates the .kotlin_metadata files to ensure reflection in Kotlin does not break
	kotlinRelocate("kotlin", "shadow.kotlin")
	kotlinRelocate("okhttp3", "shadow.okhttp3")
	kotlinRelocate("okio", "shadow.okio")
	kotlinRelocate("retrofit", "shadow.retrofit")
	doLast("revertKotlinPackageChanges") { revertKotlinPackageChanges(this as ShadowJar) }
}

// Defer the resolution of 'runtimeClasspath'. This is an issue in the shadow
// plugin that it automatically accesses the files in 'runtimeClasspath' while
// Gradle is building the task graph. The lines below work around that.
// https://github.com/GradleUp/shadow/issues/882
tasks.withType<ShadowJar> {
	dependsOn(tasks.jar)
	inputs.files(project.configurations.runtimeClasspath)
	configurations = emptyList()
	doFirst { configurations = listOf(project.configurations.runtimeClasspath.get()) }
}
