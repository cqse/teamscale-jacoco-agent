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
import eu.cqse.teamscale.report.testwise.closure.ClosureTestwiseCoverageGenerator;
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportGenerator;
import eu.cqse.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.report.testwise.model.TestExecution;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverage;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverageReport;
import eu.cqse.teamscale.test.listeners.JUnit5TestListenerExtension;
import org.junit.platform.console.options.ImpactedTestsExecutorCommandLineOptions;
import org.junit.platform.console.options.TestExecutorCommandLineOptionsParser;
import org.junit.platform.console.tasks.TestDetailsCollector;
import org.junit.platform.console.tasks.TestExecutor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import retrofit2.Response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportUtils.writeReportToFile;

/**
 * The {@code ImpactedTestsExecutor} is a stand-alone application for executing impacted tests
 * and collecting testwise coverage written for the JUnit platform.
 */
public class ImpactedTestsExecutor {

	/** Logger used to print status to the console during test retrieval and execution. */
	private final Logger logger;

	/** The main entry point for the impacted tests executor. */
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
			commandLineOptionsParser.printHelp(logger.output);
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

		return executeTests(options, availableTestDetails);
	}

	/** Discovers all tests that match the given filters in #options. */
	private List<TestDetails> getTestDetails(ImpactedTestsExecutorCommandLineOptions options) {
		List<TestDetails> availableTestDetails = new TestDetailsCollector(logger).collect(options);

		logger.info("Found " + availableTestDetails.size() + " tests");

		// Write out test details to file
		if (options.getReportsDir().isPresent()) {
			writeTestDetailsReport(options.getReportsDir().get().toFile(), availableTestDetails);
		}
		return availableTestDetails;
	}

	/** Executes either all tests if set via the command line options or queries Teamscale for the impacted tests and executes those. */
	private ConsoleLauncherExecutionResult executeTests(ImpactedTestsExecutorCommandLineOptions options, List<TestDetails> availableTestDetails) {
		TestExecutor testExecutor = new TestExecutor(options, logger);
		TestExecutionSummary testExecutionSummary;
		if (options.runAllTests) {
			testExecutionSummary = testExecutor.executeAllTests();
		} else {
			List<String> impactedTests = getImpactedTestsFromTeamscale(availableTestDetails, options);
			if (impactedTests == null) {
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

		File reportFile = new File(reportDir, "test-list.json");
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(reportFile)))) {
			out.print(new GsonBuilder().setPrettyPrinting().create().toJson(testDetails));
		} catch (IOException e) {
			// We don't want to break the tests because writing the testDetails failed.
			logger.error(e);
		}
	}

	/** Queries Teamscale for impacted tests. */
	private List<String> getImpactedTestsFromTeamscale(List<TestDetails> availableTestDetails, ImpactedTestsExecutorCommandLineOptions options) {
		try {
			logger.output.println("Getting impacted tests...");
			TeamscaleClient client = new TeamscaleClient(options.server.url, options.server.userName,
					options.server.userAccessToken, options.server.project);
			Response<List<String>> response = client
					.getImpactedTests(availableTestDetails, options.baseline, options.endCommit, options.partition);
			if (response.isSuccessful()) {
				List<String> testList = response.body();
				if (testList == null) {
					logger.error("Teamscale was not able to determine impacted tests.");
				}
				return testList;
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
