import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class TiaAgent {

	private final boolean includeNonImpactedTests;
	private final ITestwiseCoverageAgentApi api;

	public TiaAgent(boolean includeNonImpactedTests, HttpUrl url) {
		this.includeNonImpactedTests = includeNonImpactedTests;
		api = ITestwiseCoverageAgentApi.createService(url);
	}

	public TestRun startTestRun(List<ClusteredTestDetails> availableTests) throws AgentHttpRequestFailedException {
		List<PrioritizableTestCluster> clusters = handleRequestError(
				api.testRunStarted(includeNonImpactedTests, availableTests), "Failed to start the test run");
		return new TestRun(api, clusters);
	}

	public static class AgentHttpRequestFailedException extends Exception {

		public AgentHttpRequestFailedException(String message) {
			super(message);
		}

		public AgentHttpRequestFailedException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	private static <T> T handleRequestError(Call<T> request,
											String errorMessage) throws AgentHttpRequestFailedException {
		try {
			Response<T> response = request.execute();
			if (response.isSuccessful()) {
				return response.body();
			}

			ResponseBody errorBody = response.errorBody();
			String bodyString = "<no response body sent>";
			if (errorBody != null) {
				bodyString = errorBody.string();
			}
			throw new AgentHttpRequestFailedException(
					errorMessage + ". The agent responded with HTTP status " + response.code() + " " + response
							.message() + ". Response body: " + bodyString);
		} catch (IOException e) {
			throw new AgentHttpRequestFailedException(
					errorMessage + ". This is probably a temporary network problem.", e);
		}
	}

	public static class TestRun {

		private final ITestwiseCoverageAgentApi api;
		private final List<PrioritizableTestCluster> testsToRun;

		private TestRun(ITestwiseCoverageAgentApi api,
						List<PrioritizableTestCluster> testsToRun) {
			this.api = api;
			this.testsToRun = testsToRun;
		}

		@FunctionalInterface
		public interface TestRunner {
			TestResultWithMessage run(PrioritizableTest test) throws Exception;
		}

		public static class TestResultWithMessage {
			public final ETestExecutionResult result;
			public final String message;

			public TestResultWithMessage(ETestExecutionResult result, String message) {
				this.result = result;
				this.message = message;
			}
		}

		public void runPrioritizedTests(TestRunner runner) throws AgentHttpRequestFailedException {
			List<TestExecution> executions = new ArrayList<>();

			for (PrioritizableTestCluster cluster : testsToRun) {
				for (PrioritizableTest test : cluster.tests) {
					executions.add(runTest(runner, test));
				}
			}

			handleRequestError(api.testRunFinished(executions),
					"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
		}

		private TestExecution runTest(TestRunner runner, PrioritizableTest test) {
			long startTime = System.currentTimeMillis();
			TestExecution execution;

			try {
				handleRequestError(api.testStarted(test.uniformPath),
						"Failed to start coverage recording for test case " + test.uniformPath);
				TestResultWithMessage result = runner.run(test);
				long endTime = System.currentTimeMillis();

				handleRequestError(api.testFinished(test.uniformPath),
						"Failed to end coverage recording for test case " + test.uniformPath +
								". Coverage recording is most likely still running for that test case");
				execution = new TestExecution(test.uniformPath, endTime - startTime, result.result, result.message);
			} catch (Exception e) {
				long endTime = System.currentTimeMillis();
				StringWriter writer = new StringWriter();
				try (PrintWriter printWriter = new PrintWriter(writer)) {
					e.printStackTrace(printWriter);
				}
				execution = new TestExecution(test.uniformPath, endTime - startTime,
						ETestExecutionResult.ERROR, writer.toString());
			}
			return execution;
		}

	}

}
