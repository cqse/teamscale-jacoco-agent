package eu.cqse.teamscale.jacoco.javaws;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Wraps javaws and adds the profiler via `-J-javaagent`. */
public class Main {

	/** Visible for testing. */
	/* package */ static final String PROPERTY_ARGENT_ARGUMENTS = "argentArguments";
	/** Visible for testing. */
	/* package */ static final String PROPERTY_JAVAWS = "javaws";
	/** Visible for testing. */
	/* package */ static final String PROPERTIES_FILENAME = "javaws.properties";

	/** Entry point. */
	public static void main(String[] args) throws InterruptedException, ConfigurationException, IOException {
		Path workingDirectory = Paths.get(System.getProperty("user.dir"));
		new Main().run(args, workingDirectory);
	}

	/**
	 * Contains the actual logic to run the wrapper.
	 */
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
		String additionalAgentArguments = readProperty(properties, PROPERTY_ARGENT_ARGUMENTS, configFile);

		String agentArgument = buildAgentArgument(workingDirectory, additionalAgentArguments);
		String policyArgument = buildPolicyArgument(workingDirectory);

		List<String> commandLine = new ArrayList<>(Arrays.asList(args));
		commandLine.add(0, pathToJavaws);
		commandLine.add(1, agentArgument);
		commandLine.add(2, policyArgument);

		System.out.println("Running real javaws command: " + commandLine);

		int exitCode = Main.runCommand(commandLine);
		System.exit(exitCode);
	}

	private String buildPolicyArgument(Path workingDirectory) {
		Path policyFile = workingDirectory.resolve("agent.policy").toAbsolutePath();
		return "-J-Djava.security.policy=" + policyFile;
	}

	/**
	 * Runs the given command line and returns the exit code. Stdout and Stderr are
	 * redirected to System.out/System.err.
	 */
	public static int runCommand(List<String> commandLine) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(commandLine);
		Process process = builder.inheritIO().start();
		return process.waitFor();
	}

	private static String readProperty(Properties properties, String property, Path configFile)
			throws ConfigurationException {
		String additionalAgentArguments = properties.getProperty(property, null);
		if (additionalAgentArguments == null) {
			throw new ConfigurationException("Missing property `" + property + "` in config file " + configFile);
		}
		return additionalAgentArguments;
	}

	private static String buildAgentArgument(Path workingDirectory, String additionalAgentArguments)
			throws IOException {
		Path agentJar = workingDirectory.resolve("agent.jar").toAbsolutePath();

		Path tempDirectory = Files.createTempDirectory(workingDirectory, "classdumpdir");
		tempDirectory.toFile().deleteOnExit();

		return "-J-javaagent:" + agentJar + "=class-dir=" + tempDirectory + ",jacoco-classdumpdir=" + tempDirectory
				+ "," + additionalAgentArguments;
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
