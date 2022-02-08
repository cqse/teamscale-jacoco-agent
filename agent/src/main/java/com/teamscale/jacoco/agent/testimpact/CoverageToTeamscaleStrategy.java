package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.EReportFormat;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Strategy that records test-wise coverage and uploads the resulting report to Teamscale. Also handles the {@link
 * #testRunStart(List, boolean, boolean, boolean, String)} event by retrieving tests to run from Teamscale.
 */
public class CoverageToTeamscaleStrategy extends TestEventHandlerStrategyBase {

	private final Logger logger = LoggingUtils.getLogger(this);

	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	/**
	 * The path to the exec file into which the coverage of the current test run is appended to. Will be null if there
	 * is no file for the current test run yet.
	 */
	private File testExecFile;
	private final List<TestExecution> testExecutions = new ArrayList<>();
	private List<ClusteredTestDetails> availableTests = new ArrayList<>();
	private final JaCoCoTestwiseReportGenerator reportGenerator;

	public CoverageToTeamscaleStrategy(JacocoRuntimeController controller, AgentOptions agentOptions,
									   JaCoCoTestwiseReportGenerator reportGenerator) {
		super(agentOptions, controller);
		this.reportGenerator = reportGenerator;

		if (!agentOptions.getTeamscaleServerOptions().hasCommitOrRevision()) {
			throw new UnsupportedOperationException(
					"You must provide a commit or revision via the agent's '" + AgentOptions.TEAMSCALE_COMMIT_OPTION +
							"', '" + AgentOptions.TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION + "', '" +
							AgentOptions.TEAMSCALE_REVISION_OPTION + "' or '" +
							AgentOptions.TEAMSCALE_GIT_PROPERTIES_JAR_OPTION + "' option." +
							" Auto-detecting the git.properties does not work since we need the commit before any code" +
							" has been profiled in order to obtain the prioritized test cases from the TIA.");
		}
	}

	@Override
	public String testRunStart(List<ClusteredTestDetails> availableTests, boolean includeNonImpactedTests, boolean includeAddedTests, boolean includeFailedAndSkipped,
							   String baseline) throws IOException {
		if (availableTests != null) {
			this.availableTests = new ArrayList<>(availableTests);
		}
		return super.testRunStart(this.availableTests, includeNonImpactedTests, includeAddedTests, includeFailedAndSkipped, baseline);
	}

	@Override
	public void testStart(String uniformPath) {
		super.testStart(uniformPath);

		if (availableTests.stream().noneMatch(test -> test.uniformPath.equals(uniformPath))) {
			// ensure that we can at least generate a report for the tests that were actually run,
			// even if the caller did not provide a list of tests up-front in testRunStart
			availableTests.add(new ClusteredTestDetails(uniformPath, uniformPath, null, null));
		}
	}

	@Override
	public String testEnd(String test,
						  TestExecution testExecution) throws JacocoRuntimeController.DumpException, CoverageGenerationException {
		super.testEnd(test, testExecution);
		testExecutions.add(testExecution);

		try {
			if (testExecFile == null) {
				testExecFile = agentOptions.createTempFile("coverage", "exec");
				testExecFile.deleteOnExit();
			}
			controller.dumpToFileAndReset(testExecFile);
		} catch (IOException e) {
			throw new JacocoRuntimeController.DumpException(
					"Failed to write coverage to disk into " + testExecFile + "!",
					e);
		}
		return null;
	}

	@Override
	public void testRunEnd() throws IOException, CoverageGenerationException {
		if (testExecFile == null) {
			logger.warn("Tried to end a test run that contained no tests!");
			return;
		}

		String testwiseCoverageJson = createTestwiseCoverageReport();
		try {
			teamscaleClient
					.uploadReport(EReportFormat.TESTWISE_COVERAGE, testwiseCoverageJson,
							agentOptions.getTeamscaleServerOptions().commit,
							agentOptions.getTeamscaleServerOptions().revision,
							agentOptions.getTeamscaleServerOptions().partition,
							agentOptions.getTeamscaleServerOptions().getMessage());
		} catch (IOException e) {
			File reportFile = agentOptions.createTempFile("testwise-coverage", "json");
			FileSystemUtils.writeFileUTF8(reportFile, testwiseCoverageJson);
			String errorMessage = "Failed to upload coverage to Teamscale! Report is stored in " + reportFile + "!";
			logger.error(errorMessage, e);
			throw new IOException(errorMessage, e);
		}
	}

	/**
	 * Creates a testwise coverage report from the coverage collected in {@link #testExecFile} and the test execution
	 * information in {@link #testExecutions}.
	 */
	private String createTestwiseCoverageReport() throws IOException, CoverageGenerationException {
		List<String> executionUniformPaths = testExecutions.stream().map(execution -> {
			if (execution == null) {
				return null;
			} else {
				return execution.getUniformPath();
			}
		}).collect(toList());

		logger.debug("Creating testwise coverage form available tests `{}`, test executions `{}` and exec file",
				availableTests.stream().map(test -> test.uniformPath).collect(toList()),
				executionUniformPaths);
		TestwiseCoverage testwiseCoverage = reportGenerator.convert(testExecFile);
		logger.debug("Created testwise coverage report (containing coverage for tests `{}`)",
				testwiseCoverage.getTests().stream().map(TestCoverageBuilder::getUniformPath).collect(toList()));

		TestwiseCoverageReport report = TestwiseCoverageReportBuilder
				.createFrom(availableTests, testwiseCoverage.getTests(), testExecutions);

		testExecFile.delete();
		testExecFile = null;
		availableTests.clear();
		testExecutions.clear();

		return testwiseCoverageReportJsonAdapter.toJson(report);
	}

}
