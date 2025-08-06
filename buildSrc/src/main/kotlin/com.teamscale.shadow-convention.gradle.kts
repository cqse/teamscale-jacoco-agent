import com.github.jengelman.gradle.plugins.shadow.tasks.DependencyFilter
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
	enableAutoRelocation = project.properties["debug"] !== "true"
	archiveClassifier = null as String?
	mergeServiceFiles()
	// Relocates the .kotlin_metadata files to ensure reflection in Kotlin does not break
	kotlinRelocate("kotlin", "shadow.kotlin")
	kotlinRelocate("okhttp3", "shadow.okhttp3")
	kotlinRelocate("okio", "shadow.okio")
	kotlinRelocate("retrofit", "shadow.retrofit")
	val archiveFile = this.archiveFile
	doLast("revertKotlinPackageChanges") { revertKotlinPackageChanges(archiveFile) }
}

// Defer the resolution of 'runtimeClasspath'. This is an issue in the shadow
// plugin that it automatically accesses the files in 'runtimeClasspath' while
// Gradle is building the task graph. The lines below work around that.
// https://github.com/GradleUp/shadow/issues/882
tasks.withType<ShadowJar> {
	// Do not resolve too early through 'dependencyFilter'
	dependencyFilter = NoResolveDependencyFilter(project)
}

class NoResolveDependencyFilter(
	project: Project
) : DependencyFilter.AbstractDependencyFilter(project) {

	// Copy of https://github.com/GradleUp/shadow/blob/main/src/main/kotlin/com/github/jengelman/gradle/plugins/shadow/internal/DefaultDependencyFilter.kt#L10
	override fun resolve(
		dependencies: Set<ResolvedDependency>,
		includedDependencies: MutableSet<ResolvedDependency>,
		excludedDependencies: MutableSet<ResolvedDependency>,
	) {
		dependencies.forEach {
			if (if (it.isIncluded()) includedDependencies.add(it) else excludedDependencies.add(it)) {
				resolve(it.children, includedDependencies, excludedDependencies)
			}
		}
	}

	override fun resolve(configuration: Configuration): FileCollection {
		return configuration
	}
}
