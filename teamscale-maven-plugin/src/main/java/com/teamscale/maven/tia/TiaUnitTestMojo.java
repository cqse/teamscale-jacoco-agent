package com.teamscale.maven.tia;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Instruments the Surefire unit tests and uploads testwise coverage to Teamscale.
 */
@Mojo(name = "prepare-tia-unit-test", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME,
		threadSafe = true)
public class TiaUnitTestMojo extends TiaMojoBase {

	/**
	 * The partition to which to upload unit test coverage.
	 */
	@Parameter(defaultValue = "Unit Tests")
	public String partition;

	@Override
	protected String getPartition() {
		return partition;
	}

	@Override
	protected boolean isIntegrationTest() {
		return false;
	}

	@Override
	protected String getTestPluginArtifact() {
		return "org.apache.maven.plugins:maven-surefire-plugin";
	}
}
