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
		// If a Dom is null it means the execution goal uses default parameters which work correctly
		Xpp3Dom reportConfigurationDom = getExecutionConfigurationDom(JACOCO_PLUGIN_NAME, "report");
		validateReportFormat(reportConfigurationDom, "report");
		Xpp3Dom reportIntegrationConfigurationDom = getExecutionConfigurationDom(JACOCO_PLUGIN_NAME, "report-integration");
		validateReportFormat(reportIntegrationConfigurationDom, "report-integration");
		Xpp3Dom reportAggregateConfigurationDom = getExecutionConfigurationDom(JACOCO_PLUGIN_NAME, "report-aggregate");
		validateReportFormat(reportAggregateConfigurationDom, "report-aggregate");
	}

	/**
	 * Validates that a configuration Dom is set up to generate XML reports
	 * @param configurationDom The configuration Dom of a goal execution
	 * @param pluginGoal The name of the goal
	 * @throws MojoFailureException If the goal is not set up to generate XML reports
	 */
	private void validateReportFormat(Xpp3Dom configurationDom, String pluginGoal) throws MojoFailureException {
		if (configurationDom != null && configurationDom.getChild("formats") != null) {
			boolean producesXMLReport = false;
			for (Xpp3Dom format : configurationDom.getChild("formats").getChildren()) {
				if (format.getValue().equals("XML")) {
					producesXMLReport = true;
					break;
				}
			}
			if (!producesXMLReport) {
				throw new MojoFailureException(JACOCO_PLUGIN_NAME + " is not configured to produce XML reports for goal " + pluginGoal);
			}
		}
	}
}
