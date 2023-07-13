package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Base for strategies that produce testwise coverage information
 * in JSON and store or send this data further.
 */
public abstract class CoverageToJsonStrategyBase extends TestEventHandlerStrategyBase{

	/**
	 * The logger to use.
	 */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/**
	 * The path to the exec file into which the coverage of the current test run is appended to. Will be null if there
	 * is no file for the current test run yet.
	 */
	private File testExecFile;
	private final List<TestExecution> testExecutions = new ArrayList<>();
	private List<ClusteredTestDetails> availableTests = new ArrayList<>();

	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	private final JaCoCoTestwiseReportGenerator reportGenerator;

	public CoverageToJsonStrategyBase(JacocoRuntimeController controller, AgentOptions agentOptions,
									   JaCoCoTestwiseReportGenerator reportGenerator) {
		super(agentOptions, controller);
		this.reportGenerator = reportGenerator;
	}

	@Override
	public List<PrioritizableTestCluster> testRunStart(List<ClusteredTestDetails> availableTests,
													   boolean includeNonImpactedTests,
													   boolean includeAddedTests, boolean includeFailedAndSkipped,
													   String baseline) throws IOException {
		if (availableTests != null) {
			this.availableTests = new ArrayList<>(availableTests);
		}
		return super.testRunStart(this.availableTests, includeNonImpactedTests, includeAddedTests,
				includeFailedAndSkipped, baseline);
	}

	@Override
	public void testStart(String uniformPath) {
		super.testStart(uniformPath);

		if (availableTests.stream().noneMatch(test -> test.uniformPath.equals(uniformPath))) {
			// ensure that we can at least generate a report for the tests that were actually run,
			// even if the caller did not provide a list of tests up-front in testRunStart
			availableTests.add(new ClusteredTestDetails(uniformPath, uniformPath, null, null, null));
		}
	}

	@Override
	public TestInfo testEnd(String test,
							TestExecution testExecution) throws JacocoRuntimeController.DumpException, CoverageGenerationException {
		super.testEnd(test, testExecution);
		if (testExecution != null) {
			testExecutions.add(testExecution);
		}

		try {
			if (testExecFile == null) {
				testExecFile = agentOptions.createNewFileInOutputDirectory("coverage", "exec");
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
	public void testRunEnd(boolean partial) throws IOException, CoverageGenerationException {
		if (testExecFile == null) {
			logger.warn("Tried to end a test run that contained no tests!");
			return;
		}

		String testwiseCoverageJson = createTestwiseCoverageReport(partial);
		handleTestwiseCoverageJsonReady(testwiseCoverageJson);
	}

	/**
	 * Hook that is invoked when the JSON is ready for processed further.
	 */
	protected abstract void handleTestwiseCoverageJsonReady(String json) throws IOException;

	/**
	 * Creates a testwise coverage report from the coverage collected in {@link #testExecFile} and the test execution
	 * information in {@link #testExecutions}.
	 */
	private String createTestwiseCoverageReport(boolean partial) throws IOException, CoverageGenerationException {
		List<String> executionUniformPaths = testExecutions.stream().map(execution -> {
			if (execution == null) {
				return null;
			} else {
				return execution.getUniformPath();
			}
		}).collect(toList());

		logger.debug(
				"Creating testwise coverage from available tests `{}`, test executions `{}`, exec file and partial {}",
				availableTests.stream().map(test -> test.uniformPath).collect(toList()),
				executionUniformPaths, partial);
		reportGenerator.updateClassDirCache();
		TestwiseCoverage testwiseCoverage = reportGenerator.convert(testExecFile);
		logger.debug("Created testwise coverage report (containing coverage for tests `{}`)",
				testwiseCoverage.getTests().stream().map(TestCoverageBuilder::getUniformPath).collect(toList()));

		TestwiseCoverageReport report = TestwiseCoverageReportBuilder
				.createFrom(availableTests, testwiseCoverage.getTests(), testExecutions, partial);

		testExecFile.delete();
		testExecFile = null;
		availableTests.clear();
		testExecutions.clear();

		return testwiseCoverageReportJsonAdapter.toJson(report);
	}

}
