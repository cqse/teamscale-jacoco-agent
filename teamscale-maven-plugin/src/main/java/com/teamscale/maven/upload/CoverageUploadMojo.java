package com.teamscale.maven.upload;

import com.teamscale.maven.TeamscaleMojoBase;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import shadow.com.teamscale.client.CommitDescriptor;
import shadow.com.teamscale.client.EReportFormat;
import shadow.com.teamscale.client.TeamscaleClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * Binds to the VERIFY phase in which the Jacoco plugin generates its report.
 * Needs to be specified after the Jacoco goal to ensure that it is run once the Jacoco report goal has completed.
 * Offers the following functionality:
 * <ol>
 *     <li>Validate Jacoco Maven plugin configuration</li>
 *     <li>Locate and upload all reports in one session</li>
 * </ol>
 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/maven.html">Jacoco Plugin</a>
 */
@Mojo(name = "upload-coverage", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.RUNTIME,
		threadSafe = true)
public class CoverageUploadMojo extends TeamscaleMojoBase {

	private final static String JACOCO_PLUGIN_NAME = "org.jacoco:jacoco-maven-plugin";

	/**
	 * The directory where Jacoco creates directories for its report by default, usually "./target/site"
	 */
	@Parameter(defaultValue = "${project.reporting.outputDirectory}")
	private String reportDirectory;

	/**
	 * The directory in which Jacoco will place its reports
	 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/report-mojo.html">report</a>
	 */
	private Path reportGoalOutputDirectory;

	/**
	 * The directory in which Jacoco will place its reports for the report-integration goal
	 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/report-integration-mojo.html">report-integration</a>
	 */
	private Path reportIntegrationGoalOutputDirectory;

	/**
	 * The directory in which Jacoco will place its reports for the report-aggregate goal
	 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/report-aggregate-mojo.html">report-aggregate</a>
	 */
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
		getLog().debug("Resolving end commit");
		resolveEndCommit();
		getLog().debug("Parsing Jacoco plugin configuration");
		parseJacocoConfiguration();
		try {
			getLog().debug("Uploading coverage reports");
			uploadCoverageReports();
		} catch (IOException e) {
			throw new MojoFailureException("Uploading coverage reports failed: " + e);
		}
	}

	/**
	 * Check that Jacoco is set up correctly and read any custom settings that may have been set
	 * @throws MojoFailureException If Jacoco is not set up correctly
	 */
	private void parseJacocoConfiguration() throws MojoFailureException {
		// If a Dom is null it means the execution goal uses default parameters which work correctly
		Xpp3Dom reportConfigurationDom = getJacocoGoalExecutionConfiguration("report");
		validateReportFormat(reportConfigurationDom, "report");
		reportGoalOutputDirectory = getCustomOutputDirectory(reportConfigurationDom).orElse(Paths.get(reportDirectory).resolve("jacoco"));

		Xpp3Dom reportIntegrationConfigurationDom = getJacocoGoalExecutionConfiguration("report-integration");
		validateReportFormat(reportIntegrationConfigurationDom, "report-integration");
		reportIntegrationGoalOutputDirectory = getCustomOutputDirectory(reportIntegrationConfigurationDom).orElse(Paths.get(reportDirectory).resolve("jacoco-it"));

		Xpp3Dom reportAggregateConfigurationDom = getJacocoGoalExecutionConfiguration("report-aggregate");
		validateReportFormat(reportAggregateConfigurationDom, "report-aggregate");
		reportAggregateGoalOutputDirectory = getCustomOutputDirectory(reportAggregateConfigurationDom).orElse(Paths.get(reportDirectory).resolve("jacoco-aggregate"));
	}

	private void uploadCoverageReports() throws IOException {
		uploadCoverage(reportGoalOutputDirectory, "Unit Tests");
		uploadCoverage(reportIntegrationGoalOutputDirectory, "Integration Tests");
		uploadCoverage(reportAggregateGoalOutputDirectory, "Aggregated Reports");
	}

	private void uploadCoverage(Path reportOutputDirectory, String partition) throws IOException {
		File jacocoReport = reportOutputDirectory.resolve("jacoco.xml").toFile();
		if (!jacocoReport.exists() || !jacocoReport.canRead()) {
			getLog().debug(String.format("%s does not exist or is not accessible", jacocoReport.toPath()));
			return;
		}
		String report = Arrays.toString(Files.readAllBytes(jacocoReport.toPath()));
		getLog().debug(String.format("Uploading Jacoco report for project %s to %s", projectId, partition));
		teamscaleClient.uploadReport(EReportFormat.JACOCO, report, CommitDescriptor.parse(resolvedEndCommit), revision, partition, "External upload via Teamscale Maven plugin");
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
