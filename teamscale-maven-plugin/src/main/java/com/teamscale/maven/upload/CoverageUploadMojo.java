package com.teamscale.maven.upload;

import com.teamscale.maven.TeamscaleMojoBase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Binds to the VERIFY phase in which the Jacoco plugin generates its report.
 * Needs to be specified after the Jacoco goal to ensure that it is run once the Jacoco report goal has completed.
 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/report-mojo.html">Jacoco Report goal</a>
 */
@Mojo(name = "upload-coverage", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.RUNTIME,
		threadSafe = true)
public class CoverageUploadMojo extends TeamscaleMojoBase {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
	}
}
