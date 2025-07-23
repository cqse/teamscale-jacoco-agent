import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
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
	enableRelocation = project.properties["debug"] !== "true"
	archiveClassifier = null as String?
	mergeServiceFiles()
	// Relocates the .kotlin_metadata files to ensure reflection in Kotlin does not break
	kotlinRelocate("kotlin", "shadow.kotlin")
	kotlinRelocate("okhttp3", "shadow.okhttp3")
	kotlinRelocate("okio", "shadow.okio")
	kotlinRelocate("retrofit", "shadow.retrofit")
	doLast("revertKotlinPackageChanges") { revertKotlinPackageChanges(this as ShadowJar) }
}

// https://github.com/GradleUp/shadow/issues/1501
configurations {
	named(ShadowJavaPlugin.SHADOW_RUNTIME_ELEMENTS_CONFIGURATION_NAME) {
		attributes {
			attributeProvider(
				TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
				configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
					.map { it.attributes.getAttribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE)!! }
			)
		}
	}
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
) : DefaultDependencyFilter(project) {
	override fun resolve(configuration: Configuration): FileCollection {
		return configuration
	}
}

// Copy of https://github.com/GradleUp/shadow/blob/main/src/main/kotlin/com/github/jengelman/gradle/plugins/shadow/internal/DefaultDependencyFilter.kt
open class DefaultDependencyFilter(
	project: Project,
) : AbstractDependencyFilter(project) {
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
}

// Copy of https://github.com/GradleUp/shadow/blob/main/src/main/kotlin/com/github/jengelman/gradle/plugins/shadow/internal/AbstractDependencyFilter.kt
abstract class AbstractDependencyFilter(
	@Transient private val project: Project,
	@Transient protected val includeSpecs: MutableList<Spec<ResolvedDependency>> = mutableListOf(),
	@Transient protected val excludeSpecs: MutableList<Spec<ResolvedDependency>> = mutableListOf(),
) : DependencyFilter {

	protected abstract fun resolve(
		dependencies: Set<ResolvedDependency>,
		includedDependencies: MutableSet<ResolvedDependency>,
		excludedDependencies: MutableSet<ResolvedDependency>,
	)

	override fun resolve(configuration: Configuration): FileCollection {
		val included = mutableSetOf<ResolvedDependency>()
		val excluded = mutableSetOf<ResolvedDependency>()
		resolve(configuration.resolvedConfiguration.firstLevelModuleDependencies, included, excluded)
		return project.files(configuration.files) -
				project.files(excluded.flatMap { it.moduleArtifacts.map(ResolvedArtifact::getFile) })
	}

	override fun resolve(configurations: Collection<Configuration>): FileCollection {
		return configurations.map { resolve(it) }
			.reduceOrNull { acc, fileCollection -> acc + fileCollection }
			?: project.files()
	}

	override fun exclude(spec: Spec<ResolvedDependency>) {
		excludeSpecs.add(spec)
	}

	override fun include(spec: Spec<ResolvedDependency>) {
		includeSpecs.add(spec)
	}

	override fun project(notation: Any): Spec<ResolvedDependency> {
		@Suppress("UNCHECKED_CAST")
		val realNotation = when (notation) {
			is ProjectDependency -> return notation.toSpec()
			is Provider<*> -> mapOf("path" to notation.get())
			is String -> mapOf("path" to notation)
			is Map<*, *> -> notation as Map<String, Any>
			else -> throw IllegalArgumentException("Unsupported notation type: ${notation::class.java}")
		}
		return project.dependencies.project(realNotation).toSpec()
	}

	override fun dependency(dependencyNotation: Any): Spec<ResolvedDependency> {
		val realNotation = when (dependencyNotation) {
			is Provider<*> -> dependencyNotation.get()
			else -> dependencyNotation
		}
		return project.dependencies.create(realNotation).toSpec()
	}

	protected fun ResolvedDependency.isIncluded(): Boolean {
		val include = includeSpecs.isEmpty() || includeSpecs.any { it.isSatisfiedBy(this) }
		val exclude = excludeSpecs.isNotEmpty() && excludeSpecs.any { it.isSatisfiedBy(this) }
		return include && !exclude
	}

	private fun Dependency.toSpec(): Spec<ResolvedDependency> {
		return Spec<ResolvedDependency> { resolvedDependency ->
			(group == null || resolvedDependency.moduleGroup.matches(group!!.toRegex())) &&
					resolvedDependency.moduleName.matches(name.toRegex()) &&
					(version == null || resolvedDependency.moduleVersion.matches(version!!.toRegex()))
		}
	}
}
