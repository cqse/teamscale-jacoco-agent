package com.teamscale.tia.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Instruments the Failsafe integration tests and uploads testwise coverage to Teamscale.
 */
@Mojo(name = "prepare-tia-integration-test", defaultPhase = LifecyclePhase.PACKAGE,
		requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class TiaIntegrationTestMojo extends TiaMojoBase {

	/**
	 * The partition to which to upload integration test coverage.
	 */
	@Parameter(defaultValue = "Integration Tests")
	public String partition;

	@Override
	protected String getPartition() {
		return partition;
	}

	@Override
	protected boolean isIntegrationTest() {
		return true;
	}

	@Override
	protected String getTestPluginArtifact() {
		return "org.apache.maven.plugins:maven-failsafe-plugin";
	}
}
