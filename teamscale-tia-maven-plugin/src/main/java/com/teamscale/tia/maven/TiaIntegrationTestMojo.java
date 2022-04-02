package com.teamscale.tia.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "tia-it", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
		requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class TiaIntegrationTestMojo extends TiaMojoBase {

	@Parameter(defaultValue = "Integration Tests")
	public String partition;

	@Override
	public String getPartition() {
		return partition;
	}
}
