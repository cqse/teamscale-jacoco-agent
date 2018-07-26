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
import org.junit.platform.console.tasks.CustomConsoleTestExecutor;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import retrofit2.Response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

/**
 * The {@code CustomConsoleLauncher} is a stand-alone application for launching the
 * JUnit Platform from the console.
 *
 * @since 1.0
 */
public class CustomConsoleLauncher {

	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_RED = "\u001B[31m";

	private final JOptSimpleCustomCommandLineOptionsParser commandLineOptionsParser;
	private final PrintStream outStream;
	private final PrintStream errStream;
	private final Charset charset;

	public static void main(String... args) {
		int exitCode = execute(System.out, System.err, args).getExitCode();
		System.exit(exitCode);
	}

	private static ConsoleLauncherExecutionResult execute(PrintStream out, PrintStream err, String... args) {
		JOptSimpleCustomCommandLineOptionsParser parser = new JOptSimpleCustomCommandLineOptionsParser();
		CustomConsoleLauncher consoleLauncher = new CustomConsoleLauncher(parser, out, err);
		return consoleLauncher.execute(args);
	}

	private CustomConsoleLauncher(JOptSimpleCustomCommandLineOptionsParser commandLineOptionsParser, PrintStream out, PrintStream err) {
		this(commandLineOptionsParser, out, err, Charset.defaultCharset());
	}

	private CustomConsoleLauncher(JOptSimpleCustomCommandLineOptionsParser commandLineOptionsParser, PrintStream out, PrintStream err,
								  Charset charset) {
		this.commandLineOptionsParser = commandLineOptionsParser;
		this.outStream = out;
		this.errStream = err;
		this.charset = charset;
	}

	private ConsoleLauncherExecutionResult execute(String... args) {
		CustomCommandLineOptions options = commandLineOptionsParser.parse(args);
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream, charset)))) {
			if (options.isDisplayHelp()) {
				commandLineOptionsParser.printHelp(out);
				return ConsoleLauncherExecutionResult.success();
			}
			return executeImpactedTests(options, out);
		} finally {
			outStream.flush();
			errStream.flush();
		}
	}

	private ConsoleLauncherExecutionResult executeImpactedTests(CustomCommandLineOptions options, PrintWriter out) {
		List<String> impactedTests;

		List<TestDetails> availableTestDetails = getTestDetails(options, out);

		if (availableTestDetails == null) {
			printRedError("Failed to load test details!");
			errStream.flush();
			outStream.println("Falling back to execute all");
			outStream.flush();
			impactedTests = null;
		} else {
			if (availableTestDetails.isEmpty()) {
				errStream.println("No tests found!");
				return ConsoleLauncherExecutionResult.success();
			}
			outStream.println("Found " + availableTestDetails.size() + " tests");
			// Write out test details to file (for debugging purposes)
			if (options.getReportsDir().isPresent()) {
				writeTestDetailsReport(options.getReportsDir().get().toFile(), availableTestDetails);
			}

			TeamscaleClient client = new TeamscaleClient(options.server);
			uploadTestDetails(options, availableTestDetails, client);
			if (options.runAllTests) {
				impactedTests = null;
			} else {
				impactedTests = getImpactedTests(client, options);
			}
		}

		try {
			TestExecutionSummary testExecutionSummary = new CustomConsoleTestExecutor(options, impactedTests)
					.execute(out);
			return ConsoleLauncherExecutionResult.forSummary(testExecutionSummary);
		} catch (Exception exception) {
			exception.printStackTrace(errStream);
			errStream.println();
			errStream.flush();
			commandLineOptionsParser.printHelp(out);
		}
		return ConsoleLauncherExecutionResult.failed();
	}

	private List<TestDetails> getTestDetails(CustomCommandLineOptions options, PrintWriter out) {
		try {
			return new CustomConsoleTestDetailsCollector(options).execute();
		} catch (Exception exception) {
			exception.printStackTrace(errStream);
			errStream.println();
			commandLineOptionsParser.printHelp(out);
		}
		return null;
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

	private List<String> getImpactedTests(TeamscaleClient client, CustomCommandLineOptions options) {
		try {
			Response<List<String>> response = client.getImpactedTests(options.endCommit, options.partition);
			if (response.isSuccessful()) {
				return response.body();
			} else {
				printRedError("Retrieval of impacted tests failed");
				errStream.println(response.code() + " " + response.message());
			}
		} catch (IOException e) {
			errStream.println(ANSI_RED + "Retrieval of impacted tests failed (" + e.getMessage() + ")" + ANSI_RESET);
		}
		return null;
	}

	private void printRedError(String error) {
		errStream.println(ANSI_RED + error + ANSI_RESET);
	}
}
