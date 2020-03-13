package com.teamscale.tia;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import okhttp3.HttpUrl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class CommandLineInterface {

	private static final JsonAdapter<List<ClusteredTestDetails>> clusteredTestDetailsJsonAdapter =
			new Moshi.Builder().build().adapter(Types.newParameterizedType(List.class, ClusteredTestDetails.class));
	private static final JsonAdapter<List<PrioritizableTestCluster>> prioritizableTestClusterJsonAdapter =
			new Moshi.Builder().build().adapter(Types.newParameterizedType(List.class, ClusteredTestDetails.class));

	private final String[] arguments;
	private final String command;
	private final ITestwiseCoverageAgentApi api;

	public CommandLineInterface(String[] arguments) {
		this.arguments = arguments;
		if (arguments.length < 2) {
			throw new RuntimeException(
					"You must provide at least two arguments: the agent's URL and the command to execute");
		}

		HttpUrl url = HttpUrl.parse(arguments[0]);
		api = ITestwiseCoverageAgentApi.createService(url);

		command = arguments[1];
	}

	public static void main(String[] arguments) throws Exception {
		new CommandLineInterface(arguments).runCommand();
	}

	private void runCommand() throws Exception {
		switch (command) {
			case "startTestRun":
				startTestRun();
				break;
			case "startTest":
				startTest();
				break;
			case "endTest":
				endTest();
				break;
			case "endTestRun":
				endTestRun();
				break;
		}
	}

	private void endTestRun() throws Exception {
		AgentCommunicationUtils.handleRequestError(api.testRunFinished(),
				"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
	}

	private void endTest() throws Exception {
		if (arguments.length < 4) {
			throw new RuntimeException("You must provide the uniform path of the test that is about to be started" +
					" as the first argument of the startTest command and the test result as the second.");
		}
		String uniformPath = arguments[2];
		ETestExecutionResult result = ETestExecutionResult.valueOf(arguments[3]);

		Long duration = getLongParameter("duration");
		if (duration == null) {
			duration = 0L;
		}

		String message = readStdin();

		TestExecution execution = new TestExecution(uniformPath, duration, result, message);
		AgentCommunicationUtils.handleRequestError(api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

	private void startTest() throws Exception {
		if (arguments.length < 3) {
			throw new RuntimeException("You must provide the uniform path of the test that is about to be started" +
					" as the first argument of the startTest command");
		}
		String uniformPath = arguments[2];
		AgentCommunicationUtils.handleRequestError(api.testStarted(uniformPath),
				"Failed to start coverage recording for test case " + uniformPath);
	}

	private void startTestRun() throws Exception {
		boolean includeNonImpacted = isSwitchPresent("includeNonImpacted");
		Long baseline = getLongParameter("baseline");

		String json = readStdin();
		List<ClusteredTestDetails> availableTests = clusteredTestDetailsJsonAdapter.fromJson(json);

		List<PrioritizableTestCluster> clusters = AgentCommunicationUtils.handleRequestError(
				api.testRunStarted(includeNonImpacted, baseline, availableTests), "Failed to start the test run");
		System.out.println(prioritizableTestClusterJsonAdapter.toJson(clusters));
	}

	private String readStdin() {
		return new BufferedReader(new InputStreamReader(System.in)).lines().collect(joining("\n"));
	}

	private Long getLongParameter(String name) {
		return Stream.of(arguments)
				.filter(argument -> argument.startsWith("--" + name + "="))
				.map(argument -> argument.substring(name.length() + 3))
				.map(Long::parseLong)
				.findFirst()
				.orElse(null);
	}

	private boolean isSwitchPresent(String name) {
		return Stream.of(arguments).anyMatch(argument -> argument.equals("--" + name));
	}

}
