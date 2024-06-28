package com.teamscale.jacoco.agent.testimpact;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.conqat.lib.commons.filesystem.FileSystemUtils;

import com.teamscale.client.EReportFormat;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleConfig;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;

/**
 * Strategy that records test-wise coverage and uploads the resulting report to Teamscale. Also handles the
 * {@link #testRunStart(List, boolean, boolean, boolean, String)} event by retrieving tests to run from Teamscale.
 */
public class CoverageToTeamscaleStrategy extends CoverageToJsonStrategyBase {

	public CoverageToTeamscaleStrategy(JacocoRuntimeController controller, AgentOptions agentOptions,
									   JaCoCoTestwiseReportGenerator reportGenerator) {
		super(controller, agentOptions, reportGenerator);

		if (!agentOptions.getTeamscaleServerOptions().hasCommitOrRevision()) {
			throw new UnsupportedOperationException(
					"You must provide a commit or revision via the agent's '" + TeamscaleConfig.TEAMSCALE_COMMIT_OPTION +
							"', '" + TeamscaleConfig.TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION + "', '" +
							TeamscaleConfig.TEAMSCALE_REVISION_OPTION + "' or '" +
							AgentOptions.GIT_PROPERTIES_JAR_OPTION + "' option." +
							" Auto-detecting the git.properties does not work since we need the commit before any code" +
							" has been profiled in order to obtain the prioritized test cases from the TIA.");
		}
	}

	@Override
	protected void handleTestwiseCoverageJsonReady(String json) throws IOException {
		try {
			teamscaleClient
					.uploadReport(EReportFormat.TESTWISE_COVERAGE, json,
							agentOptions.getTeamscaleServerOptions().commit,
							agentOptions.getTeamscaleServerOptions().revision,
							agentOptions.getTeamscaleServerOptions().repository,
							agentOptions.getTeamscaleServerOptions().partition,
							agentOptions.getTeamscaleServerOptions().getMessage());
		} catch (IOException e) {
			File reportFile = agentOptions.createNewFileInOutputDirectory("testwise-coverage", "json");
			FileSystemUtils.writeFileUTF8(reportFile, json);
			String errorMessage = "Failed to upload coverage to Teamscale! Report is stored in " + reportFile + "!";
			logger.error(errorMessage, e);
			throw new IOException(errorMessage, e);
		}
	}
}
