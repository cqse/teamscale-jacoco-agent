package com.teamscale.maven.tia;

import com.teamscale.maven.TeamscaleMojoBase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

/**
 * Base class for TIA Mojos. Provides all necessary functionality but can be subclassed to change the partition.
 * <p>
 * For this plugin to work, you must either
 *
 * <ul>
 *     <li>Make Surefire and Failsafe use our JUnit 5 test engine</li>
 *     <li>Send test start and end events to the Java agent themselves</li>
 * </ul>
 * <p>
 * To use our JUnit 5 test engine, you must declare it as a dependency of the maven-surefire-plugin and/or the
 * maven-failsafe-plugin. Example:
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>org.apache.maven.plugins</groupId>
 *     <artifactId>maven-surefire-plugin</artifactId>
 *     <version>3.0.0-M5</version>
 *     <dependencies>
 *         <dependency>
 *             <groupId>com.teamscale</groupId>
 *             <artifactId>teamscale-surefire-provider</artifactId>
 *             <version>23.2.0</version>
 *         </dependency>
 *     </dependencies>
 * </plugin>
 * }</pre>
 * <p>
 * To send test events yourself, you can use our TIA client library (Maven coordinates: com.teamscale:tia-client).
 * <p>
 * The log file of the agent is written to {@code ${project.build.directory}/tia/agent.log}.
 */
public abstract class TiaMojoBase extends TeamscaleMojoBase {

	/**
	 * You can optionally specify which code should be included in the coverage instrumentation. Each pattern is applied
	 * to the fully qualified class names of the profiled system. Use {@code *} to match any number characters and
	 * {@code ?} to match any single character.
	 * <p>
	 * Classes that match any of the include patterns are included, unless any exclude pattern excludes them.
	 */
	@Parameter
	public String[] includes;

	/**
	 * You can optionally specify which code should be excluded from the coverage instrumentation. Each pattern is
	 * applied to the fully qualified class names of the profiled system. Use {@code *} to match any number characters
	 * and {@code ?} to match any single character.
	 * <p>
	 * Classes that match any of the exclude patterns are excluded, even if they are included by an include pattern.
	 */
	@Parameter
	public String[] excludes;

	/**
	 * In order to instrument the system under test, a Java agent must be attached to the JVM of the system. The JVM
	 * command line arguments to achieve this are by default written to the property {@code argLine}, which is
	 * automatically picked up by Surefire and Failsafe and applied to the JVMs these plugins start. You can override
	 * the name of this property if you wish to manually apply the command line arguments yourself, e.g. if your system
	 * under test is started by some other plugin like the Spring boot starter.
	 */
	@Parameter
	public String propertyName;

	/**
	 * Port on which the Java agent listens for commands from this plugin.
	 */
	@Parameter(defaultValue = "12888")
	public String agentPort;

	/**
	 * Optional additional arguments to send to the agent. Each argument must be of the form {@code KEY=VALUE}.
	 */
	@Parameter
	public String[] additionalAgentOptions;

	/**
	 * Changes the log level of the agent to DEBUG.
	 */
	@Parameter(defaultValue = "false")
	public boolean debugLogging;

	private Path targetDirectory;

	@Override
	public void execute() throws MojoFailureException {
		if (skip) {
			return;
		}

		validateTestPluginConfiguration(getTestPluginArtifact());

		targetDirectory = Paths.get(projectBuildDir, "tia").toAbsolutePath();
		createTargetDirectory();

		resolvedEndCommit = resolveEndCommit();

		setTiaProperty("reportDirectory", targetDirectory.toString());
		setTiaProperty("server.url", teamscaleUrl);
		setTiaProperty("server.project", projectId);
		setTiaProperty("server.userName", username);
		setTiaProperty("server.userAccessToken", accessToken);
		setTiaProperty("endCommit", resolvedEndCommit);
		setTiaProperty("partition", getPartition());
		setTiaProperty("agentsUrls", "http://localhost:" + agentPort);

		Path agentConfigFile = createAgentConfigFiles();
		Path logFilePath = targetDirectory.resolve("agent.log");
		setArgLine(agentConfigFile, logFilePath);
	}

	private void validateTestPluginConfiguration(String testPluginArtifact) throws MojoFailureException {
		Xpp3Dom configurationDom = getConfigurationDom(testPluginArtifact);
		if (configurationDom == null) return;

		validateParallelizationParameter(testPluginArtifact, configurationDom, "threadCount");
		validateParallelizationParameter(testPluginArtifact, configurationDom, "forkCount");

		Xpp3Dom parameterDom = configurationDom.getChild("reuseForks");
		if (parameterDom == null) {
			return;
		}

		String value = parameterDom.getValue();
		if (value != null && !value.equals("true")) {
			throw new MojoFailureException(
					"You configured the " + testPluginArtifact + " plugin to not reuse forks via the reuseForks configuration parameter." +
							" This is not supported when performing Test Impact analysis as it prevents properly recording testwise coverage." +
							" Please enable fork reuse when running Test Impact analysis.");
		}
	}

	private void validateParallelizationParameter(String testPluginArtifact, Xpp3Dom configurationDom,
						   String parallelizationParameter) throws MojoFailureException {
		Xpp3Dom parameterDom = configurationDom.getChild(parallelizationParameter);
		if (parameterDom == null) {
			return;
		}

		String value = parameterDom.getValue();
		if (value != null && !value.equals("1")) {
			throw new MojoFailureException(
					"You configured parallel tests in the " + testPluginArtifact + " plugin via the " + parallelizationParameter + " configuration parameter." +
							" Parallel tests are not supported when performing Test Impact analysis as they prevent recording testwise coverage." +
							" Please disable parallel tests when running Test Impact analysis.");
		}
	}

	/**
	 * @return the partition to upload testwise coverage to.
	 */
	protected abstract String getPartition();

	/**
	 * @return the artifact name of the test plugin (e.g. Surefire, Failsafe).
	 */
	protected abstract String getTestPluginArtifact();

	/**
	 * @return whether this Mojo applies to integration tests.
	 * <p>
	 * Depending on this, different properties are used to set the argLine.
	 */
	protected abstract boolean isIntegrationTest();

	private void createTargetDirectory() throws MojoFailureException {
		try {
			Files.createDirectories(targetDirectory);
		} catch (IOException e) {
			throw new MojoFailureException("Could not create target directory " + targetDirectory, e);
		}
	}

	private void setArgLine(Path agentConfigFile, Path logFilePath) {
		String agentLogLevel = "INFO";
		if (debugLogging) {
			agentLogLevel = "DEBUG";
		}

		ArgLine.cleanOldArgLines(session, getLog());
		ArgLine.applyToMavenProject(
				new ArgLine(additionalAgentOptions, agentLogLevel, findAgentJarFile(), agentConfigFile, logFilePath),
				session, getLog(), propertyName, isIntegrationTest());
	}

	private Path createAgentConfigFiles() throws MojoFailureException {
		Path loggingConfigPath = targetDirectory.resolve("logback.xml");
		try (OutputStream loggingConfigOutputStream = Files.newOutputStream(loggingConfigPath)) {
			FileSystemUtils.copy(readAgentLogbackConfig(), loggingConfigOutputStream);
		} catch (IOException e) {
			throw new MojoFailureException("Writing the logging configuration file for the TIA agent failed." +
					" Make sure the path " + loggingConfigPath + " is writeable.", e);
		}

		Path configFilePath = targetDirectory.resolve("agent.properties");
		String agentConfig = createAgentConfig(loggingConfigPath, targetDirectory.resolve("reports"));
		try {
			Files.write(configFilePath, Collections.singleton(agentConfig));
		} catch (IOException e) {
			throw new MojoFailureException("Writing the configuration file for the TIA agent failed." +
					" Make sure the path " + configFilePath + " is writeable.", e);
		}

		getLog().info("Agent config file created at " + configFilePath);
		return configFilePath;
	}

	private InputStream readAgentLogbackConfig() {
		return TiaMojoBase.class.getResourceAsStream("logback-agent.xml");
	}

	private String createAgentConfig(Path loggingConfigPath, Path agentOutputDirectory) {
		String config = "mode=testwise" +
				"\ntia-mode=teamscale-upload" +
				"\nteamscale-server-url=" + teamscaleUrl +
				"\nteamscale-project=" + projectId +
				"\nteamscale-user=" + username +
				"\nteamscale-access-token=" + accessToken +
				"\nteamscale-commit=" + resolvedEndCommit +
				"\nteamscale-partition=" + getPartition() +
				"\nhttp-server-port=" + agentPort +
				"\nlogging-config=" + loggingConfigPath +
				"\nout=" + agentOutputDirectory.toAbsolutePath();
		if (ArrayUtils.isNotEmpty(includes)) {
			config += "\nincludes=" + String.join(",", includes);
		}
		if (ArrayUtils.isNotEmpty(excludes)) {
			config += "\nexcludes=" + String.join(",", excludes);
		}
		return config;
	}

	private Path findAgentJarFile() {
		Artifact agentArtifact = pluginArtifactMap.get("com.teamscale:teamscale-jacoco-agent");
		return agentArtifact.getFile().toPath();
	}

	/**
	 * Sets a property in the TIA namespace. It seems that, depending on Maven version and which other plugins are used,
	 * different types of properties are respected both during the build and during tests (as e.g. failsafe tests are
	 * often run in a separate JVM spawned by Maven). So we set our properties in every possible way to make sure the
	 * plugin works out of the box in most situations.
	 */
	private void setTiaProperty(String name, String value) {
		String fullyQualifiedName = "teamscale.test.impacted." + name;
		getLog().debug("Setting property " + name + "=" + value);
		session.getUserProperties().setProperty(fullyQualifiedName, value);
		session.getSystemProperties().setProperty(fullyQualifiedName, value);
		System.setProperty(fullyQualifiedName, value);
	}
}
