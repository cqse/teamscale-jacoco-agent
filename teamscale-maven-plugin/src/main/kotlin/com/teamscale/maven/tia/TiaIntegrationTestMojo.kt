package com.teamscale.maven.tia

import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Instruments the Failsafe integration tests and uploads testwise coverage to Teamscale.
 */
@Mojo(
	name = "prepare-tia-integration-test",
	defaultPhase = LifecyclePhase.PACKAGE,
	requiresDependencyResolution = ResolutionScope.RUNTIME,
	threadSafe = true
)
class TiaIntegrationTestMojo : TiaMojoBase() {
	/**
	 * The partition to which to upload integration test coverage.
	 */
	@Parameter(defaultValue = "Integration Tests")
	override lateinit var partition: String

	override val isIntegrationTest = true
	override val testPluginArtifact = "org.apache.maven.plugins:maven-failsafe-plugin"
	override val testPluginPropertyPrefix = "failsafe"
}
