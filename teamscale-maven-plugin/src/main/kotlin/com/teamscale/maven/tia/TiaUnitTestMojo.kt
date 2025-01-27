package com.teamscale.maven.tia

import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Instruments the Surefire unit tests and uploads testwise coverage to Teamscale.
 */
@Mojo(
	name = "prepare-tia-unit-test",
	defaultPhase = LifecyclePhase.INITIALIZE,
	requiresDependencyResolution = ResolutionScope.RUNTIME,
	threadSafe = true
)
class TiaUnitTestMojo : TiaMojoBase() {
	/**
	 * The partition to which to upload unit test coverage.
	 */
	@Parameter(defaultValue = "Unit Tests")
	override lateinit var partition: String

	override val isIntegrationTest = false
	override val testPluginArtifact = "org.apache.maven.plugins:maven-surefire-plugin"
	override val testPluginPropertyPrefix = "surefire"
}
