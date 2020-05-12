package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.EReportFormat;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Strategy that records test-wise coverage and uploads the resulting report to Teamscale. Also handles the {@link
 * #testRunStart(List, boolean, Long)} event by retrieving tests to run from Teamscale.
 */
public class CoverageToTeamscaleStrategy extends TestEventHandlerStrategyBase {

	private final Logger logger = LoggingUtils.getLogger(this);

	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	private final TestwiseCoverage testwiseCoverage = new TestwiseCoverage();
	private final List<TestExecution> testExecutions = new ArrayList<>();
	private List<ClusteredTestDetails> availableTests = new ArrayList<>();
	private final JaCoCoTestwiseReportGenerator reportGenerator;

	public CoverageToTeamscaleStrategy(JacocoRuntimeController controller, AgentOptions agentOptions,
									   JaCoCoTestwiseReportGenerator reportGenerator) {
		super(agentOptions, controller);
		this.reportGenerator = reportGenerator;

		if (!agentOptions.getTeamscaleServerOptions().hasCommitOrRevision()) {
			throw new UnsupportedOperationException("You must provide a commit or revision via the agent's options." +
					" Auto-detecting the git.properties does not work since we need the commit before any code" +
					" has been profiled in order to obtain the prioritized test cases from the TIA.");
		}
	}

	@Override
	public String testRunStart(List<ClusteredTestDetails> availableTests, boolean includeNonImpactedTests,
							   Long baseline) throws IOException {
		this.availableTests = availableTests;
		return super.testRunStart(availableTests, includeNonImpactedTests, baseline);
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
		Dump dump = controller.dumpAndReset();
		testwiseCoverage.add(reportGenerator.convert(dump));
		return null;
	}

	@Override
	public void testRunEnd() throws IOException {
		List<String> executionUniformPaths = testExecutions.stream().map(execution -> {
			if (execution == null) {
				return null;
			} else {
				return execution.getUniformPath();
			}
		}).collect(toList());
		logger.debug("Creating coverage for available tests `{}`, test executions `{}` and coverage for `{}`",
				availableTests.stream().map(test -> test.uniformPath).collect(toList()),
				executionUniformPaths,
				testwiseCoverage.getTests().stream().map(TestCoverageBuilder::getUniformPath).collect(toList()));

		TestwiseCoverageReport report = TestwiseCoverageReportBuilder
				.createFrom(availableTests, testwiseCoverage.getTests(), testExecutions);

		String json = testwiseCoverageReportJsonAdapter.toJson(report);
		teamscaleClient
				.uploadReport(EReportFormat.TESTWISE_COVERAGE, json, agentOptions.getTeamscaleServerOptions().commit,
						agentOptions.getTeamscaleServerOptions().revision,
						agentOptions.getTeamscaleServerOptions().partition,
						agentOptions.getTeamscaleServerOptions().message);
	}

}
