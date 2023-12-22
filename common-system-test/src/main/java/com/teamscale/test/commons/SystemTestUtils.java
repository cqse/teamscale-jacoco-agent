package com.teamscale.test.commons;

import com.teamscale.report.testwise.model.TestInfo;
import org.conqat.lib.commons.io.ProcessUtils;
import org.conqat.lib.commons.system.SystemUtils;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.POST;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for running system tests.
 */
public class SystemTestUtils {

	/**
	 * The port for the mock Teamscale server that was picked by the Gradle build script and is guaranteed to not
	 * conflict with other system tests.
	 */
	public static final Integer TEAMSCALE_PORT = Integer.getInteger("teamscalePort");

	/**
	 * The port for the agent that was picked by the Gradle build script and is guaranteed to not conflict with other
	 * system tests.
	 */
	public static final Integer AGENT_PORT = Integer.getInteger("agentPort");

	/**
	 * Turns the coverage of the given {@link TestInfo} into a string for simple assertions.
	 * <p>
	 * Example: {@code file1.java:1,7-12;file2.java:9-22,33}
	 */
	public static String getCoverageString(TestInfo info) {
		return info.paths.stream().flatMap(path -> path.getFiles().stream())
				.map(file -> file.fileName + ":" + file.coveredLines).collect(
						Collectors.joining(";"));
	}

	/**
	 * Runs the clean and verify goal of the Maven project at the given path.
	 *
	 * @throws IOException if running Maven fails.
	 */
	public static void runMavenTests(String mavenProjectPath) throws IOException {
		File workingDirectory = new File(mavenProjectPath);

		ProcessUtils.ExecutionResult result;
		try {
			List<String> arguments = new ArrayList<>();
			if (SystemUtils.isWindows()) {
				Collections.addAll(arguments, "cmd", "/c", "mvnw.cmd");
			} else {
				arguments.add("./mvnw");
			}

			arguments.add("clean");
			arguments.add("verify");

			result = ProcessUtils.execute(new ProcessBuilder(arguments).directory(workingDirectory));
		} catch (IOException e) {
			throw new IOException(
					"Failed to run ./mvnw clean verify in directory " + workingDirectory.getAbsolutePath(),
					e);
		}

		// in case the process succeeded, we still log stdout and stderr in case later assertions fail. This helps
		// debug test failures
		System.out.println("Maven stdout: " + result.getStdout());
		System.out.println("Maven stderr: " + result.getStderr());

		if (result.terminatedByTimeoutOrInterruption()) {
			throw new IOException("Running Maven failed: " + result.getStdout() + "\n" + result.getStderr());
		}
	}

	/** Retrieve all files in the `tia/reports` folder sorted by name. */
	public static List<Path> getReportFileNames(String mavenProjectPath) throws IOException {
		try (Stream<Path> stream = Files.walk(Paths.get(mavenProjectPath, "target", "tia", "reports"))) {
			return stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
		}
	}

	private interface AgentService {
		/** Dumps coverage */
		@POST("/dump")
		Call<Void> dump();
	}

	/** Instructs the agent via HTTP to dump the currently collected coverage. */
	public static void dumpCoverage(int agentPort) throws IOException {
		new Retrofit.Builder().baseUrl("http://localhost:" + agentPort).build()
				.create(AgentService.class).dump().execute();
	}

}
