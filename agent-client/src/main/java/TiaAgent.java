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
import java.util.List;

public class TiaAgent {

	private final boolean includeNonImpactedTests;
	private final ITestwiseCoverageAgentApi api;

	public TiaAgent(boolean includeNonImpactedTests, HttpUrl url) {
		this.includeNonImpactedTests = includeNonImpactedTests;
		api = ITestwiseCoverageAgentApi.createService(url);
	}

	public TestRun startTestRun(List<ClusteredTestDetails> availableTests,
								long baseline) throws AgentHttpRequestFailedException {
		List<PrioritizableTestCluster> clusters = handleRequestError(
				api.testRunStarted(includeNonImpactedTests, baseline, availableTests), "Failed to start the test run");
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
		private final List<PrioritizableTestCluster> prioritizedTests;

		private TestRun(ITestwiseCoverageAgentApi api,
						List<PrioritizableTestCluster> prioritizedTests) {
			this.api = api;
			this.prioritizedTests = prioritizedTests;
		}

		public List<PrioritizableTestCluster> getPrioritizedTests() {
			return prioritizedTests;
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

		public RunningTest startTest(PrioritizableTest test) throws AgentHttpRequestFailedException {
			handleRequestError(api.testStarted(test.uniformPath),
					"Failed to start coverage recording for test case " + test.uniformPath);
			return new RunningTest(test, api);
		}

		public void endTestRun() throws AgentHttpRequestFailedException {
			handleRequestError(api.testRunFinished(),
					"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
		}

	}


	public static class RunningTest {

		private final PrioritizableTest test;
		private final ITestwiseCoverageAgentApi api;
		private final long startTime = System.currentTimeMillis();

		public RunningTest(PrioritizableTest test, ITestwiseCoverageAgentApi api) {
			this.test = test;
			this.api = api;
		}

		public void endTestWithException(Throwable throwable) throws AgentHttpRequestFailedException {
			long endTime = System.currentTimeMillis();

			StringWriter writer = new StringWriter();
			try (PrintWriter printWriter = new PrintWriter(writer)) {
				throwable.printStackTrace(printWriter);
			}
			TestExecution execution = new TestExecution(test.uniformPath, endTime - startTime,
					ETestExecutionResult.ERROR, throwable.getMessage() + "\n" + writer.toString());

			handleRequestError(api.testFinished(test.uniformPath, execution),
					"Failed to end coverage recording for test case " + test.uniformPath +
							". Coverage for that test case is most likely lost.");
		}

		public void endTestNormally(TestRun.TestResultWithMessage result) throws AgentHttpRequestFailedException {
			long endTime = System.currentTimeMillis();
			TestExecution execution = new TestExecution(test.uniformPath, endTime - startTime, result.result,
					result.message);
			handleRequestError(api.testFinished(test.uniformPath, execution),
					"Failed to end coverage recording for test case " + test.uniformPath +
							". Coverage for that test case is most likely lost.");
		}

	}

}
