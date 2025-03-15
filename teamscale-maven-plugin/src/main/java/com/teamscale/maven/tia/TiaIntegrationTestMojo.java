package com.teamscale.maven.tia;

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

	/** The name of the directory into which the binary execution data is written. */
	public static final String OUTPUT_DIR_NAME = "tia-integration";

	/**
	 * The Teamscale partition name to which integration test reports will be uploaded.
	 */
	@Parameter(property = "teamscale.integrationTestPartition", defaultValue = "Integration Tests")
	public String integrationTestPartition;

	@Override
	protected String getPartition() {
		return integrationTestPartition;
	}

	@Override
	protected boolean isIntegrationTest() {
		return true;
	}

	@Override
	protected String getTestPluginArtifact() {
		return "org.apache.maven.plugins:maven-failsafe-plugin";
	}

	@Override
	protected String getTestPluginPropertyPrefix() {
		return "failsafe";
	}

	@Override
	protected String getOutputDirectoryName() {
		return OUTPUT_DIR_NAME;
	}
}
