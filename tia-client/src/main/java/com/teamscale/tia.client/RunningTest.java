package com.teamscale.tia.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import com.teamscale.client.StringUtils;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Represents a single test that is currently being executed by the caller of this library. Use {@link
 * #endTest(TestRun.TestResultWithMessage)} or {@link #endTestAndRetrieveCoverage(TestRun.TestResultWithMessage)} to
 * signal that executing the test case has finished and test-wise coverage for this test should be stored.
 */
@SuppressWarnings("unused")
public class RunningTest {

	private final String uniformPath;
	private final ITestwiseCoverageAgentApi api;
	private final JsonAdapter<TestInfo> testInfoJsonAdapter = new Moshi.Builder().build().adapter(TestInfo.class);

	public RunningTest(String uniformPath, ITestwiseCoverageAgentApi api) {
		this.uniformPath = uniformPath;
		this.api = api;
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test and the result of the test run.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTest(TestRun.TestResultWithMessage result) throws AgentHttpRequestFailedException {
		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L, result.result,
				result.message);
		AgentCommunicationUtils.handleRequestError(() -> api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test and the result of the test run. It
	 * will also parse the testwise coverage data returned by the agent for this test and return it so it can be
	 * manually processed by you. The agent will not store or otherwise process this coverage, so be sure to do so
	 * yourself.
	 * <p>
	 * This method assumes that the agent is configured to return each test's coverage data via HTTP. It will receive
	 * and parse the data. If the agent is configured differently, this method will throw a terminal {@link
	 * RuntimeException}. In this case, you must reconfigure the agent with the `coverage-via-http` option enabled.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The recorded coverage is likely lost. The caller should log this
	 *                                         problem appropriately.
	 */
	public TestInfo endTestAndRetrieveCoverage(
			TestRun.TestResultWithMessage result) throws AgentHttpRequestFailedException {
		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L, result.result,
				result.message);
		ResponseBody body = AgentCommunicationUtils.handleRequestError(() -> api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");

		String json;
		try {
			json = body.string();
		} catch (IOException e) {
			throw new AgentHttpRequestFailedException("Unable to read response body string", e);
		}

		if (StringUtils.isBlank(json)) {
			throw new RuntimeException("You asked the tia-client to retrieve this test's coverage via HTTP but the" +
					" agent is not configured for this. Please reconfigure the agent to use `coverage-via-http`.");
		}

		try {
			return testInfoJsonAdapter.fromJson(json);
		} catch (JsonDataException | IOException e) {
			throw new AgentHttpRequestFailedException("Unable to parse the JSON returned by the agent. Maybe you have" +
					" a version mismatch between the tia-client and the agent?. Json returned by the agent: `" + json +
					"`", e);
		}
	}

}
