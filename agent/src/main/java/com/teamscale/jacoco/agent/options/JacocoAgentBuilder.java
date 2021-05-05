package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.Agent;
import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.testimpact.TestExecutionWriter;
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.util.AgentUtils;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/** Builder for the JaCoCo agent options string. */
public class JacocoAgentBuilder {
	private final Logger logger = LoggingUtils.getLogger(this);

	private final AgentOptions agentOptions;

	public JacocoAgentBuilder(AgentOptions agentOptions) {
		this.agentOptions = agentOptions;
	}

	/**
	 * Returns the options to pass to the JaCoCo agent.
	 */
	public String createJacocoAgentOptions() throws AgentOptionParseException, IOException {
		StringBuilder builder = new StringBuilder(getModeSpecificOptions());
		if (agentOptions.jacocoIncludes != null) {
			builder.append(",includes=").append(agentOptions.jacocoIncludes);
		}
		if (agentOptions.jacocoExcludes != null) {
			builder.append(",excludes=").append(agentOptions.jacocoExcludes);
		}

		// Don't dump class files in testwise mode when coverage is written to an exec file
		boolean needsClassFiles = agentOptions.mode == EMode.NORMAL || agentOptions.testwiseCoverageMode != ETestwiseCoverageMode.EXEC_FILE;
		if (agentOptions.classDirectoriesOrZips.isEmpty() && needsClassFiles) {
			Path tempDir = createTemporaryDumpDirectory();
			tempDir.toFile().deleteOnExit();
			builder.append(",classdumpdir=").append(tempDir.toAbsolutePath().toString());

			agentOptions.classDirectoriesOrZips = Collections.singletonList(tempDir.toFile());
		}

		agentOptions.additionalJacocoOptions
				.forEach((key, value) -> builder.append(",").append(key).append("=").append(value));

		return builder.toString();
	}

	private Path createTemporaryDumpDirectory() throws AgentOptionParseException {
		try {
			return Files.createTempDirectory("jacoco-class-dump");
		} catch (IOException e) {
			logger.warn("Unable to create temporary directory in default location. Trying in output directory.");
		}

		try {
			return Files.createTempDirectory(agentOptions.getOutputDirectory(), "jacoco-class-dump");
		} catch (IOException e) {
			logger.warn("Unable to create temporary directory in output directory. Trying in agent's directory.");
		}

		Path agentDirectory = AgentUtils.getAgentDirectory();
		if (agentDirectory == null) {
			throw new AgentOptionParseException("Could not resolve directory that contains the agent");
		}
		try {
			return Files.createTempDirectory(agentDirectory, "jacoco-class-dump");
		} catch (IOException e) {
			throw new AgentOptionParseException("Unable to create a temporary directory anywhere", e);
		}
	}

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	public AgentBase createAgent(Instrumentation instrumentation) throws UploaderException, IOException {
		if (agentOptions.useTestwiseCoverageMode()) {
			JaCoCoTestwiseReportGenerator reportGenerator = new JaCoCoTestwiseReportGenerator(
					agentOptions.getClassDirectoriesOrZips(), agentOptions.getLocationIncludeFilter(),
					agentOptions.getDuplicateClassFileBehavior(), LoggingUtils.wrap(logger));
			return new TestwiseCoverageAgent(agentOptions,
					new TestExecutionWriter(agentOptions.createTempFile("test-execution", "json")),
					reportGenerator);
		} else {
			return new Agent(agentOptions, instrumentation);
		}
	}

	/**
	 * Returns additional options for JaCoCo depending on the selected {@link AgentOptions#mode} and {@link
	 * AgentOptions#testwiseCoverageMode}.
	 */
	String getModeSpecificOptions() throws IOException {
		if (agentOptions
				.useTestwiseCoverageMode() && agentOptions.testwiseCoverageMode == ETestwiseCoverageMode.EXEC_FILE) {
			String sessionId = "";
			if (agentOptions.testEnvironmentVariable != null) {
				sessionId = System.getenv(agentOptions.testEnvironmentVariable);
			}
			// when writing to a .exec file, we can instruct JaCoCo to do so directly
			return "sessionid=" + sessionId + ",destfile=" + agentOptions.createTempFile("jacoco", "exec").getAbsolutePath();

		} else {
			// otherwise we don't need JaCoCo to perform any output of the .exec information
			return "output=none";
		}
	}

}
