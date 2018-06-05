package eu.cqse.teamscale.jacoco.javaws;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;

/** Wraps javaws and adds the profiler via `-J-javaagent`. */
public class Main {

	/** Visible for testing. */
	/* package */ static final String PROPERTY_AGENT_ARGUMENTS = "agentArguments";
	/** Visible for testing. */
	/* package */ static final String PROPERTY_JAVAWS = "javaws";
	/** Visible for testing. */
	/* package */ static final String PROPERTIES_FILENAME = "javaws.properties";
	private static final String JAVA_TOOL_OPTIONS_VARIABLE = "JAVA_TOOL_OPTIONS";

	/** Entry point. */
	public static void main(String[] args)
			throws InterruptedException, ConfigurationException, IOException, URISyntaxException {
		URI jarFileUri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		// jar file is located inside the lib folder. Config files are one level higher
		Path workingDirectory = Paths.get(jarFileUri).getParent().getParent();
		new Main().run(args, workingDirectory);
	}

	/** Contains the actual logic to run the wrapper. */
	private void run(String[] args, Path workingDirectory)
			throws InterruptedException, ConfigurationException, IOException {
		Path configFile = workingDirectory.resolve(PROPERTIES_FILENAME).toAbsolutePath();

		Properties properties;
		try {
			properties = readProperties(configFile);
		} catch (IOException e) {
			throw new ConfigurationException("Unable to read config file " + configFile);
		}

		String pathToJavaws = readProperty(properties, PROPERTY_JAVAWS, configFile);
		String additionalAgentArguments = readProperty(properties, PROPERTY_AGENT_ARGUMENTS, configFile);

		String agentArgument = buildAgentArgument(workingDirectory, additionalAgentArguments);
		String policyArgument = buildPolicyArgument(workingDirectory);

		List<String> commandLine = new ArrayList<>(Arrays.asList(args));
		commandLine.add(0, pathToJavaws);
		commandLine.add(1, policyArgument);

		System.out.println("Running real javaws command: " + commandLine);
		System.out.println("With environment variable " + JAVA_TOOL_OPTIONS_VARIABLE + "=" + agentArgument);

		int exitCode = Main.runCommand(commandLine,
				Collections.singletonMap(JAVA_TOOL_OPTIONS_VARIABLE, agentArgument));
		System.exit(exitCode);
	}

	private String buildPolicyArgument(Path workingDirectory) {
		Path policyFile = workingDirectory.resolve("agent.policy");
		return "-J-Djava.security.policy=" + normalizePath(policyFile);
	}

	/**
	 * Runs the given command line and returns the exit code. Stdout and Stderr are
	 * redirected to System.out/System.err.
	 */
	public static int runCommand(List<String> commandLine, Map<String, String> environmentVariables)
			throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(commandLine);
		builder.environment().putAll(environmentVariables);
		Process process = builder.inheritIO().start();
		return process.waitFor();
	}

	private static String readProperty(Properties properties, String property, Path configFile)
			throws ConfigurationException {
		String value = properties.getProperty(property, null);
		if (value == null) {
			throw new ConfigurationException("Missing property `" + property + "` in config file " + configFile);
		}
		return value;
	}

	private static String buildAgentArgument(Path workingDirectory, String additionalAgentArguments)
			throws IOException, ConfigurationException {
		String agentJarPath = normalizePath(workingDirectory.resolve("agent.jar"));

		Path tempDirectory = Files.createTempDirectory("javaws-classdumpdir");
		// we explicitly don't delete the temp directory because the javaws process will
		// exit before the actual application exits and the dir needs to be present or
		// JaCoCo will just crash
		// However, the files are created in the system's temp directory so they are
		// cleared up by the OS later in most cases
		String tempDirectoryPath = normalizePath(tempDirectory);

		if (StringUtils.isEmpty(additionalAgentArguments)) {
			throw new ConfigurationException("You must provide additional mandatory agent arguments."
					+ " At least the dump interval and a method for storing the traces must be specified");
		}

		return "-javaagent:" + agentJarPath + "=class-dir=" + tempDirectoryPath + ",jacoco-classdumpdir="
				+ tempDirectoryPath + "," + additionalAgentArguments;
	}

	/**
	 * We normalize all paths to forward slashes to avoid problems with backward
	 * slashes and escaping under Windows. Forward slashed paths still work under
	 * Windows.
	 */
	private static String normalizePath(Path path) {
		return FileSystemUtils.normalizeSeparators(path.toAbsolutePath().toString());
	}

	private static Properties readProperties(Path configFile) throws IOException, FileNotFoundException {
		Properties properties = new Properties();
		try (FileInputStream inputStream = new FileInputStream(configFile.toFile())) {
			properties.load(inputStream);
		}
		return properties;
	}

	/** Thrown if reading the config file fails. */
	public static class ConfigurationException extends Exception {

		private static final long serialVersionUID = 1L;

		public ConfigurationException(String message, Throwable cause) {
			super(message, cause);
		}

		public ConfigurationException(String message) {
			super(message);
		}

	}

}
