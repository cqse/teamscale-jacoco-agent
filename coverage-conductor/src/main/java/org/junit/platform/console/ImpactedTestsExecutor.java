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
import org.junit.platform.console.options.ImpactedTestsExecutorCommandLineOptions;
import org.junit.platform.console.options.TestExecutorCommandLineOptionsParser;
import org.junit.platform.console.tasks.TestDetailsCollector;
import org.junit.platform.console.tasks.TestExecutor;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import retrofit2.Response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * The {@code ImpactedTestsExecutor} is a stand-alone application for executing impacted tests
 * and collecting testwise coverage written for the JUnit platform.
 */
public class ImpactedTestsExecutor {

	/** Logger used to print status to the console during test retrieval and execution. */
	private final Logger logger;

	/** The entry point for the impacted tests executor. */
	public static void main(String... args) {
		try (Logger logger = new Logger(System.out, System.err)) {
			ImpactedTestsExecutor consoleLauncher = new ImpactedTestsExecutor(logger);
			int exitCode = consoleLauncher.execute(args).getExitCode();
			System.exit(exitCode);
		}
	}

	/** Constructor. */
	private ImpactedTestsExecutor(Logger logger) {
		this.logger = logger;
	}

	/** Parses the command line options and triggers discovery and execution of impacted tests. */
	private ConsoleLauncherExecutionResult execute(String... args) {
		TestExecutorCommandLineOptionsParser commandLineOptionsParser = new TestExecutorCommandLineOptionsParser();
		ImpactedTestsExecutorCommandLineOptions options = commandLineOptionsParser.parse(args);
		if (options.isDisplayHelp()) {
			commandLineOptionsParser.printHelp(logger.out);
			return ConsoleLauncherExecutionResult.success();
		}
		logger.setAnsiColorEnabled(!options.isAnsiColorOutputDisabled());
		try {
			return discoverAndExecuteTests(options);
		} catch (Exception exception) {
			logger.error(exception);
			return ConsoleLauncherExecutionResult.failed();
		}
	}

	/** Handles test detail collection and execution of the impacted tests. */
	private ConsoleLauncherExecutionResult discoverAndExecuteTests(ImpactedTestsExecutorCommandLineOptions options) {
		List<TestDetails> availableTestDetails = getTestDetails(options);
		if (availableTestDetails.isEmpty()) {
			return ConsoleLauncherExecutionResult.success();
		}

		TeamscaleClient client = new TeamscaleClient(options.server.url, options.server.userName, options.server.userAccessToken, options.server.project);
		uploadTestDetails(options, availableTestDetails, client);

		return executeTests(client, options);
	}

	/** Discovers all tests that match the given filters in #options. */
	private List<TestDetails> getTestDetails(ImpactedTestsExecutorCommandLineOptions options) {
		List<TestDetails> availableTestDetails = new TestDetailsCollector(logger).collect(options);

		logger.message("Found " + availableTestDetails.size() + " tests");

		// Write out test details to file (for debugging purposes)
		if (options.getReportsDir().isPresent()) {
			writeTestDetailsReport(options.getReportsDir().get().toFile(), availableTestDetails);
		}
		return availableTestDetails;
	}

	/** Executes either all tests if set via the command line options or queries Teamscale for the impacted tests and executes those. */
	private ConsoleLauncherExecutionResult executeTests(TeamscaleClient client, ImpactedTestsExecutorCommandLineOptions options) {
		TestExecutor testExecutor = new TestExecutor(options, logger);
		TestExecutionSummary testExecutionSummary;
		if (options.runAllTests) {
			testExecutionSummary = testExecutor.executeAllTests();
		} else {
			List<String> impactedTests = getImpactedTestsFromTeamscale(client, options);
			if(impactedTests == null) {
				testExecutionSummary = testExecutor.executeAllTests();
			} else {
				testExecutionSummary = testExecutor.executeTests(impactedTests);
			}
		}
		return ConsoleLauncherExecutionResult.forSummary(testExecutionSummary);
	}

	/** Writes the given test details to a report file. */
	private void writeTestDetailsReport(File reportDir, List<TestDetails> testDetails) {
		if (!reportDir.isDirectory() && !reportDir.mkdirs()) {
			logger.error("Failed to create directory " + reportDir.getAbsolutePath());
			return;
		}

		File reportFile = new File(reportDir, "testDetails.json");
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(reportFile)))) {
			out.print(new GsonBuilder().setPrettyPrinting().create().toJson(testDetails));
		} catch (IOException e) {
			// We don't want to break the tests because writing the testDetails failed.
			logger.error(e);
		}
	}

	/** Uploads the test details to Teamscale. */
	private void uploadTestDetails(ImpactedTestsExecutorCommandLineOptions options, List<TestDetails> availableTestDetails, TeamscaleClient client) {
		try {
			logger.message("Uploading reports to " + options.endCommit.toString() + " (" + options.partition + ")");
			client.uploadTestList(availableTestDetails, options.endCommit,
					options.partition, "Test list upload (" + options.partition + ")");
		} catch (IOException e) {
			logger.error("Test details upload failed (" + e.getMessage() + ")");
			// The test executor will fallback to execute all tests since Teamscale will return no impacted tests in
			// this case.
		}
	}

	/** Queries Teamscale for impacted tests. */
	private List<String> getImpactedTestsFromTeamscale(TeamscaleClient client, ImpactedTestsExecutorCommandLineOptions options) {
		try {
			Response<List<String>> response = client.getImpactedTests(options.baseline, options.endCommit, options.partition, logger.out);
			if (response.isSuccessful()) {
				return response.body();
			} else {
				logger.error("Retrieval of impacted tests failed");
				logger.error(response.code() + " " + response.message());
			}
		} catch (IOException e) {
			logger.error("Retrieval of impacted tests failed (" + e.getMessage() + ")");
		}
		return null;
	}
}
