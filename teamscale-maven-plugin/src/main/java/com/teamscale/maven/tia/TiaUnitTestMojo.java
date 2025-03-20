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

	/** The name of the directory into which the binary execution data is written. */
	public static final String OUTPUT_DIR_NAME = "tia";

	/**
	 * The Teamscale partition name to which unit test reports will be uploaded.
	 */
	@Parameter(property = "teamscale.unitTestPartition", defaultValue = "Unit Tests")
	public String unitTestPartition;

	@Override
	protected String getPartition() {
		return unitTestPartition;
	}

	@Override
	protected boolean isIntegrationTest() {
		return false;
	}

	@Override
	protected String getTestPluginArtifact() {
		return "org.apache.maven.plugins:maven-surefire-plugin";
	}

	@Override
	protected String getTestPluginPropertyPrefix() {
		return "surefire";
	}

	@Override
	protected String getOutputDirectoryName() {
		return OUTPUT_DIR_NAME;
	}
}
