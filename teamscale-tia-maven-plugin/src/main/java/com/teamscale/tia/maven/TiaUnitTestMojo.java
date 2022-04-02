package com.teamscale.tia.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "tia-ut", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME,
		threadSafe = true)
public class TiaUnitTestMojo extends TiaMojoBase {

	@Parameter(defaultValue = "Unit Tests")
	public String partition;

	@Override
	public String getPartition() {
		return partition;
	}
}
