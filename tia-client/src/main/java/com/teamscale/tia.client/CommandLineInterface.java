package com.teamscale.tia.client;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.utils.JsonUtils;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.utils.StringUtils;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import okhttp3.HttpUrl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Simple command-line interface to expose the {@link TiaAgent} to non-Java test runners.
 */
public class CommandLineInterface {

	private static class InvalidCommandLineException extends RuntimeException {
		public InvalidCommandLineException(String message) {
			super(message);
		}
	}

	private final List<String> arguments;
	private final String command;
	private final ITestwiseCoverageAgentApi api;

	public CommandLineInterface(String[] arguments) {
		this.arguments = new ArrayList<>(Arrays.asList(arguments));
		if (arguments.length < 2) {
			throw new InvalidCommandLineException(
					"You must provide at least two arguments: the agent's URL and the command to execute");
		}

		HttpUrl url = HttpUrl.parse(this.arguments.remove(0));
		api = ITestwiseCoverageAgentApi.createService(url);

		command = this.arguments.remove(0);
	}

	/** Entry point. */
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
			default:
				throw new InvalidCommandLineException(
						"Unknown command '" + command + "'. Should be one of startTestRun, startTest, endTest," +
								" endTestRun");
		}
	}

	private void endTestRun() throws Exception {
		boolean partial;
		if (arguments.size() == 1) {
			partial = Boolean.parseBoolean(arguments.remove(0));
		} else {
			partial = false;
		}
		AgentCommunicationUtils.handleRequestError(() -> api.testRunFinished(partial),
				"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
	}

	private void endTest() throws Exception {
		if (arguments.size() < 2) {
			throw new InvalidCommandLineException(
					"You must provide the uniform path of the test that is about to be started" +
							" as the first argument of the endTest command and the test result as the second.");
		}
		String uniformPath = arguments.remove(0);
		ETestExecutionResult result = ETestExecutionResult.valueOf(arguments.remove(0).toUpperCase());

		String message = readStdin();

		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L, result, message);
		AgentCommunicationUtils.handleRequestError(
				() -> api.testFinished(UrlUtils.percentEncode(uniformPath), execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

	private void startTest() throws Exception {
		if (arguments.size() < 1) {
			throw new InvalidCommandLineException(
					"You must provide the uniform path of the test that is about to be started" +
							" as the first argument of the startTest command");
		}
		String uniformPath = arguments.remove(0);
		AgentCommunicationUtils.handleRequestError(() -> api.testStarted(UrlUtils.percentEncode(uniformPath)),
				"Failed to start coverage recording for test case " + uniformPath +
						". Coverage for that test case is lost.");
	}

	private void startTestRun() throws Exception {
		boolean includeNonImpacted = parseAndRemoveBooleanSwitch("include-non-impacted");
		Long baseline = parseAndRemoveLongParameter("baseline");
		List<ClusteredTestDetails> availableTests = parseAvailableTestsFromStdin();

		List<PrioritizableTestCluster> clusters = AgentCommunicationUtils.handleRequestError(() ->
				api.testRunStarted(includeNonImpacted, baseline, availableTests), "Failed to start the test run");
		System.out.println(JsonUtils.serialize(clusters));
	}

	private List<ClusteredTestDetails> parseAvailableTestsFromStdin() throws java.io.IOException {
		String json = readStdin();
		List<ClusteredTestDetails> availableTests = Collections.emptyList();
		if (!StringUtils.isEmpty(json)) {
			// ToDo: Use JsonUtils.deserializeAsList()
			availableTests = JsonUtils.getOBJECT_MAPPER().readValue(
				json,
				JsonUtils.getOBJECT_MAPPER().getTypeFactory().constructCollectionLikeType(List.class, ClusteredTestDetails.class)
			);
		}
		return availableTests;
	}

	private String readStdin() {
		return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).lines()
				.collect(joining("\n"));
	}

	private Long parseAndRemoveLongParameter(String name) {
		for (int i = 0; i < arguments.size(); i++) {
			if (arguments.get(i).startsWith("--" + name + "=")) {
				String argument = arguments.remove(i);
				return Long.parseLong(argument.substring(name.length() + 3));
			}
		}
		return null;
	}

	private boolean parseAndRemoveBooleanSwitch(String name) {
		for (int i = 0; i < arguments.size(); i++) {
			if (arguments.get(i).equals("--" + name)) {
				arguments.remove(i);
				return true;
			}
		}
		return false;
	}

}
