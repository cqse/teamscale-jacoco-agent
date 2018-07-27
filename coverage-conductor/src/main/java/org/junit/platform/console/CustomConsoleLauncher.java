/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console;

import com.google.gson.GsonBuilder;
import eu.cqse.teamscale.client.TeamscaleClient;
import eu.cqse.teamscale.client.TestDetails;
import org.junit.platform.console.options.CustomCommandLineOptions;
import org.junit.platform.console.options.JOptSimpleCustomCommandLineOptionsParser;
import org.junit.platform.console.tasks.CustomConsoleTestDetailsCollector;
import org.junit.platform.console.tasks.TestExecutor;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import retrofit2.Response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * The {@code CustomConsoleLauncher} is a stand-alone application for executing impacted tests
 * and collecting testwise coverage written for the JUnit platform.
 */
public class CustomConsoleLauncher {

	/** ANSI escape sequence for writing red text to the console. */
	private static final String ANSI_RED = "\u001B[31m";

	/** ANSI escape sequence for resetting any coloring options. */
	private static final String ANSI_RESET = "\u001B[0m";

	private final PrintStream outStream;
	private final PrintStream errStream;

	public static void main(String... args) {
		int exitCode = execute(System.out, System.err, args).getExitCode();
		System.exit(exitCode);
	}

	private static ConsoleLauncherExecutionResult execute(PrintStream out, PrintStream err, String... args) {
		CustomConsoleLauncher consoleLauncher = new CustomConsoleLauncher(out, err);
		return consoleLauncher.execute(args);
	}

	private CustomConsoleLauncher(PrintStream out, PrintStream err) {
		this.outStream = out;
		this.errStream = err;
	}

	private ConsoleLauncherExecutionResult execute(String... args) {
		JOptSimpleCustomCommandLineOptionsParser commandLineOptionsParser = new JOptSimpleCustomCommandLineOptionsParser();
		CustomCommandLineOptions options = commandLineOptionsParser.parse(args);
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream)))) {
			if (options.isDisplayHelp()) {
				commandLineOptionsParser.printHelp(out);
				return ConsoleLauncherExecutionResult.success();
			}
			return discoverAndExecuteTests(options, out);
		} finally {
			outStream.flush();
			errStream.flush();
		}
	}

	/** Executes the impacted tests. */
	private ConsoleLauncherExecutionResult discoverAndExecuteTests(CustomCommandLineOptions options, PrintWriter out) {
		List<TestDetails> availableTestDetails = getTestDetails(options);

		if (availableTestDetails == null) {
			printRedError("Failed to load test details!");
			return ConsoleLauncherExecutionResult.failed();
		} else if (availableTestDetails.isEmpty()) {
			print("No tests found!");
			return ConsoleLauncherExecutionResult.success();
		} else {
			return getAndExecuteImpactedTests(options, out, availableTestDetails);
		}
	}

	/**
	 * Discovers all tests that match the given filters and uploads the test details to Teamscale.
	 */
	private List<TestDetails> getTestDetails(CustomCommandLineOptions options) {
		try {
			return new CustomConsoleTestDetailsCollector(options).execute();
		} catch (Exception exception) {
			printException(exception);
		}
		return null;
	}

	private ConsoleLauncherExecutionResult getAndExecuteImpactedTests(CustomCommandLineOptions options, PrintWriter out, List<TestDetails> availableTestDetails) {
		print("Found " + availableTestDetails.size() + " tests");

		// Write out test details to file (for debugging purposes)
		if (options.getReportsDir().isPresent()) {
			writeTestDetailsReport(options.getReportsDir().get().toFile(), availableTestDetails);
		}

		TeamscaleClient client = new TeamscaleClient(options.server);
		uploadTestDetails(options, availableTestDetails, client);
		List<String> impactedTests = null;
		if (!options.runAllTests) {
			impactedTests = getImpactedTestsFromTeamscale(client, options);
		}

		try {
			TestExecutionSummary testExecutionSummary = new TestExecutor(options, impactedTests).execute(out);
			return ConsoleLauncherExecutionResult.forSummary(testExecutionSummary);
		} catch (Exception exception) {
			printException(exception);
			return ConsoleLauncherExecutionResult.failed();
		}
	}

	private void writeTestDetailsReport(File reportDir, List<TestDetails> testDetails) {
		if (!reportDir.isDirectory() && !reportDir.mkdirs()) {
			System.err.println("Failed to create directory " + reportDir.getAbsolutePath());
			return;
		}

		File reportFile = new File(reportDir, "testDetails.json");
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(reportFile)))) {
			out.print(new GsonBuilder().setPrettyPrinting().create().toJson(testDetails));
			out.flush();
		} catch (IOException e) {
			e.printStackTrace(errStream);
		} finally {
			errStream.flush();
		}
	}

	private void uploadTestDetails(CustomCommandLineOptions options, List<TestDetails> availableTestDetails, TeamscaleClient client) {
		try {
			client.uploadTestList(availableTestDetails, options.endCommit,
					options.partition, "Test list upload (" + options.partition + ")");
		} catch (IOException e) {
			printRedError("Test details upload failed (" + e.getMessage() + ")");
		}
	}

	private List<String> getImpactedTestsFromTeamscale(TeamscaleClient client, CustomCommandLineOptions options) {
		try {
			Response<List<String>> response = client.getImpactedTests(options.endCommit, options.partition);
			if (response.isSuccessful()) {
				return response.body();
			} else {
				printRedError("Retrieval of impacted tests failed");
				printRedError(response.code() + " " + response.message());
			}
		} catch (IOException e) {
			printRedError("Retrieval of impacted tests failed (" + e.getMessage() + ")");
		}
		return null;
	}

	private void printException(Exception exception) {
		exception.printStackTrace(errStream);
		errStream.println();
		errStream.flush();
	}

	private void printRedError(String error) {
		errStream.println(ANSI_RED + error + ANSI_RESET);
		errStream.flush();
	}

	private void print(String message) {
		outStream.println(message);
		outStream.flush();
	}
}
