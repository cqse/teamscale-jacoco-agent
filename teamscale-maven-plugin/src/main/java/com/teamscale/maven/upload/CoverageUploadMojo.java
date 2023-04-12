package com.teamscale.maven.upload;

import com.teamscale.maven.TeamscaleMojoBase;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Binds to the VERIFY phase in which the Jacoco plugin generates its report.
 * Needs to be specified after the Jacoco goal to ensure that it is run once the Jacoco report goal has completed.
 * Offers the following functionality:
 * <ol>
 *     <li>Validate Jacoco Maven plugin configuration</li>
 *     <li>Locate and upload all reports in one session</li>
 * </ol>
 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/report-mojo.html">Jacoco Report goal</a>
 */
@Mojo(name = "upload-coverage", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.RUNTIME,
		threadSafe = true)
public class CoverageUploadMojo extends TeamscaleMojoBase {

	private final static String JACOCO_PLUGIN_NAME = "org.jacoco:jacoco-maven-plugin";

	@Override
	public void execute() throws MojoFailureException {
		if (skip) {
			return;
		}
		validateJacocoConfiguration();
	}

	private void validateJacocoConfiguration() throws MojoFailureException {
		Xpp3Dom configurationDom = getConfigurationDom(JACOCO_PLUGIN_NAME);
		if (configurationDom == null) {
			throw new MojoFailureException("Could not find configuration for " + JACOCO_PLUGIN_NAME);
		}
		Xpp3Dom reportFormats = configurationDom.getChild("formats");
		System.out.println(reportFormats.getValue());
	}
}
