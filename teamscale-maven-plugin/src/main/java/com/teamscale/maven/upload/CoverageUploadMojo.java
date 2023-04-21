package com.teamscale.maven.upload;

import com.teamscale.maven.TeamscaleMojoBase;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import shadow.com.teamscale.client.TeamscaleClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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

	@Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco/jacoco.xml")
	private Path reportGoalOutputDirectory;

	@Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco-it/jacoco.xml")
	private Path reportIntegrationGoalOutputDirectory;

	@Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco-aggregate/jacoco.xml")
	private Path reportAggregateGoalOutputDirectory;

	/**
	 * The Teamscale client that is used to upload reports to a Teamscale instance.
	 */
	private TeamscaleClient teamscaleClient;

	@Override
	public void execute() throws MojoFailureException {
		if (skip) {
			return;
		}
		teamscaleClient = new TeamscaleClient(teamscaleUrl, username, accessToken, projectId);
		parseJacocoConfiguration();
	}

	/**
	 * Check that Jacoco is set up correctly and read any custom settings that may have been set.
	 * @throws MojoFailureException If Jacoco is not set up correctly
	 */
	private void parseJacocoConfiguration() throws MojoFailureException {
		// If a Dom is null it means the execution goal uses default parameters which work correctly
		Xpp3Dom reportConfigurationDom = getJacocoGoalExecutionConfiguration("report");
		validateReportFormat(reportConfigurationDom, "report");
		reportGoalOutputDirectory = getCustomOutputDirectory(reportConfigurationDom).orElse(reportGoalOutputDirectory);

		Xpp3Dom reportIntegrationConfigurationDom = getJacocoGoalExecutionConfiguration("report-integration");
		validateReportFormat(reportIntegrationConfigurationDom, "report-integration");
		reportIntegrationGoalOutputDirectory = getCustomOutputDirectory(reportIntegrationConfigurationDom).orElse(reportIntegrationGoalOutputDirectory);

		Xpp3Dom reportAggregateConfigurationDom = getJacocoGoalExecutionConfiguration("report-aggregate");
		validateReportFormat(reportAggregateConfigurationDom, "report-aggregate");
		reportAggregateGoalOutputDirectory = getCustomOutputDirectory(reportAggregateConfigurationDom).orElse(reportAggregateGoalOutputDirectory);
	}

	/**
	 * Validates that a configuration Dom is set up to generate XML reports
	 * @param configurationDom The configuration Dom of a goal execution
	 * @param pluginGoal The name of the goal
	 * @throws MojoFailureException If the goal is not set up to generate XML reports
	 */
	private void validateReportFormat(Xpp3Dom configurationDom, String pluginGoal) throws MojoFailureException {
		if (configurationDom == null || configurationDom.getChild("formats") == null) {
			return;
		}
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

	private Optional<Path> getCustomOutputDirectory(Xpp3Dom configurationDom) {
		if (configurationDom != null && configurationDom.getChild("outputDirectory") != null) {
			return Optional.of(Paths.get(configurationDom.getChild("outputDirectory").getValue()));
		}
		return Optional.empty();
	}

	private Xpp3Dom getJacocoGoalExecutionConfiguration(String pluginGoal) {
		return super.getExecutionConfigurationDom(JACOCO_PLUGIN_NAME, pluginGoal);
	}
}
