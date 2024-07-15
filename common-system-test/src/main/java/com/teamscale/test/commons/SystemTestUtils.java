package com.teamscale.test.commons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.conqat.lib.commons.io.ProcessUtils;
import org.jetbrains.annotations.NotNull;

import com.teamscale.report.testwise.model.TestInfo;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

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
		runMavenTests(mavenProjectPath, new String[0]);
	}


	/**
	 * Runs the clean and verify goal of the Maven project at the given path with the provided arguments
	 *
	 * @throws IOException if running Maven fails.
	 */
	public static void runMavenTests(String mavenProjectPath, String... args) throws IOException {
		String[] allArguments = new String[2 + args.length];
		allArguments[0] = "clean";
		allArguments[1] = "verify";
		System.arraycopy(args, 0, allArguments, 2, args.length);
		runMaven(mavenProjectPath, allArguments);
	}

	/**
	 * Runs Maven in the given Maven project path with the given arguments.
	 *
	 * @throws IOException if running Maven fails.
	 */
	public static void runMaven(String mavenProjectPath, String... mavenArguments) throws IOException {
		ProcessUtils.ExecutionResult result;
		try {
			result = ProcessUtils.execute(buildMavenProcess(mavenProjectPath, mavenArguments));
		} catch (IOException e) {
			throw new IOException("Failed to run ./mvnw clean verify in directory " + mavenProjectPath, e);
		}

		// in case the process succeeded, we still log stdout and stderr in case later assertions fail. This helps
		// debug test failures
		System.out.println("Maven stdout: " + result.getStdout());
		System.out.println("Maven stderr: " + result.getStderr());

		if (result.terminatedByTimeoutOrInterruption()) {
			throw new IOException("Running Maven failed: " + result.getStdout() + "\n" + result.getStderr());
		}
	}

	/**
	 * Runs Gradle in the given Gradle project path with the given arguments.
	 *
	 * @throws IOException if running Gradle fails.
	 */
	public static void runGradle(String gradleProjectPath, String... gradleArguments) throws IOException {
		ProcessUtils.ExecutionResult result;
		try {
			result = ProcessUtils.execute(buildGradleProcess(gradleProjectPath, gradleArguments));
		} catch (IOException e) {
			throw new IOException("Failed to run ./gradlew clean verify in directory " + gradleProjectPath, e);
		}

		// in case the process succeeded, we still log stdout and stderr in case later assertions fail. This helps
		// debug test failures
		System.out.println("Gradle stdout: " + result.getStdout());
		System.out.println("Gradle stderr: " + result.getStderr());

		if (result.terminatedByTimeoutOrInterruption()) {
			throw new IOException("Running Gradle failed: " + result.getStdout() + "\n" + result.getStderr());
		}
	}

	/**
	 * Creates the command-line arguments that can be passed to {@link ProcessBuilder} to invoke Maven with the given
	 * arguments.
	 */
	@NotNull
	public static ProcessBuilder buildMavenProcess(String mavenProjectDirectory, String... mavenArguments) {
		List<String> arguments = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			Collections.addAll(arguments, "cmd", "/c", "mvnw.cmd");
		} else {
			arguments.add("./mvnw");
		}

		arguments.addAll(Arrays.asList(mavenArguments));

		return new ProcessBuilder(arguments).directory(new File(mavenProjectDirectory));
	}

	/**
	 * Creates the command-line arguments that can be passed to {@link ProcessBuilder} to invoke Gradle with the given
	 * arguments.
	 */
	@NotNull
	public static ProcessBuilder buildGradleProcess(String gradleProjectDirectory, String... gradleArguments) {
		List<String> arguments = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			Collections.addAll(arguments, "cmd", "/c", "gradlew.bat");
		} else {
			arguments.add("./gradlew");
		}

		arguments.addAll(Arrays.asList(gradleArguments));
		
		return new ProcessBuilder(arguments).directory(new File(gradleProjectDirectory));
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

		/** Changes the current partition. */
		@PUT("/partition")
		Call<Void> changePartition(@Body RequestBody newPartition);

		/** Changes the current partition. Convenience method to pass a plain string. */
		default Call<Void> changePartition(String newPartition) {
			return changePartition(RequestBody.create(MediaType.parse("text/plain"), newPartition));
		}
	}

	/** Instructs the agent via HTTP to dump the currently collected coverage. */
	public static void dumpCoverage(int agentPort) throws IOException {
		new Retrofit.Builder().baseUrl("http://localhost:" + agentPort).build()
				.create(AgentService.class).dump().execute();
	}

	/** Instructs the agent via HTTP to change the partition. */
	public static void changePartition(int agentPort, String newPartition) throws IOException {
		new Retrofit.Builder().baseUrl("http://localhost:" + agentPort).build()
				.create(AgentService.class).changePartition(newPartition).execute();
	}

}
