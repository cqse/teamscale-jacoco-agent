package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoverageToTeamscaleStrategy extends TestEventHandlerStrategyBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	private final JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportJsonAdapter = new Moshi.Builder().build()
			.adapter(TestwiseCoverageReport.class);

	private final AgentOptions agentOptions;
	private final JaCoCoTestwiseReportGenerator testwiseReportGenerator;
	private final TestwiseCoverage testwiseCoverage = new TestwiseCoverage();
	private final List<TestExecution> testExecutions = new ArrayList<>();
	private final TeamscaleClient client;
	private List<ClusteredTestDetails> availableTests = new ArrayList<>();

	public CoverageToTeamscaleStrategy(AgentOptions agentOptions,
									   JacocoRuntimeController controller) throws CoverageGenerationException {
		super(controller);
		this.agentOptions = agentOptions;

		if (agentOptions.getTeamscaleServerOptions().commit == null) {
			throw new RuntimeException("You must provide a commit via the agent's options." +
					" Auto-detecting the git.properties does not work since we need the commit before any code" +
					" has been profiled in order to obtain the prioritized test cases from TIA.");
		}

		client = agentOptions.createTeamscaleClient();
		testwiseReportGenerator = new JaCoCoTestwiseReportGenerator(
				agentOptions.getClassDirectoriesOrZips(),
				agentOptions.getLocationIncludeFilter(),
				agentOptions.getDuplicateClassFileBehavior(),
				LoggingUtils.wrap(logger));
	}

	@Override
	public void testStart(String test) {
		super.testStart(test);
	}

	@Override
	public String testEnd(String test, TestExecution testExecution) throws JacocoRuntimeController.DumpException {
		super.testEnd(test, testExecution);
		testExecutions.add(testExecution);
		Dump dump = controller.dumpAndReset();
		testwiseCoverage.add(testwiseReportGenerator.convert(dump));
		return null;
	}

	@Override
	public String testRunStart(List<ClusteredTestDetails> availableTests,
							   boolean includeNonImpactedTests, Long baseline) throws IOException {
		Response<List<PrioritizableTestCluster>> impactedTestsResponse = client
				.getImpactedTests(availableTests, baseline, agentOptions.getTeamscaleServerOptions().commit,
						agentOptions.getTeamscaleServerOptions().partition, includeNonImpactedTests);
		if (impactedTestsResponse.isSuccessful()) {
			ResponseBody rawBody = impactedTestsResponse.raw().body();
			if (rawBody == null) {
				throw new IOException("Request to Teamscale to get impacted tests failed." +
						" Teamscale did not return a response body. This is a Teamscale bug, please report it.");
			}

			return rawBody.string();
		} else {
			ResponseBody errorBody = impactedTestsResponse.errorBody();
			String responseBody = "<no response body provided>";
			if (errorBody != null) {
				responseBody = errorBody.string();
			}
			throw new IOException(
					"Request to Teamscale to get impacted tests failed with HTTP status " + impactedTestsResponse
							.code() + " " + impactedTestsResponse.message() + ". Response body: " + responseBody);
		}
	}

	@Override
	public void testRunEnd() throws IOException {
		TestwiseCoverageReport report = TestwiseCoverageReportBuilder
				// TODO (FS) should these be the available tests or the executed impacted tests?
				.createFrom(new ArrayList<>(availableTests), testwiseCoverage.getTests(), testExecutions);

		String json = testwiseCoverageReportJsonAdapter.toJson(report);
		client.uploadReport(EReportFormat.TESTWISE_COVERAGE, json, agentOptions.getTeamscaleServerOptions().commit,
				agentOptions.getTeamscaleServerOptions().partition, agentOptions.getTeamscaleServerOptions().message);
	}

	public void setAvailableTests(List<ClusteredTestDetails> availableTests) {
		this.availableTests = availableTests;
	}

}
