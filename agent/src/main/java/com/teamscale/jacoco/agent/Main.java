package com.teamscale.jacoco.agent;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.jacoco.agent.convert.ConvertCommand;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.JaCoCo;
import org.slf4j.Logger;

import java.util.ResourceBundle;

/** Provides a command line interface for interacting with JaCoCo. */
public class Main {

	/** Version of this program. */
	private static final String VERSION;

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("com.teamscale.jacoco.agent.app");
		VERSION = bundle.getString("version");
	}

	/** The logger. */
	private final Logger logger = LoggingUtils.getLogger(this);

	/** The default arguments that will always be parsed. */
	private final DefaultArguments defaultArguments = new DefaultArguments();

	/** The arguments for the one-time conversion process. */
	private final ConvertCommand command = new ConvertCommand();

	/** Entry point. */
	public static void main(String[] args) throws Exception {
		new Main().parseCommandLineAndRun(args);
	}

	/**
	 * Parses the given command line arguments. Exits the program or throws an
	 * exception if the arguments are not valid. Then runs the specified command.
	 */
	private void parseCommandLineAndRun(String[] args) throws Exception {
		Builder builder = createJCommanderBuilder();
		JCommander jCommander = builder.build();

		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			handleInvalidCommandLine(jCommander, e.getMessage());
		}

		if (defaultArguments.help) {
			System.out.println("CQSE JaCoCo agent " + VERSION + " compiled against JaCoCo " + JaCoCo.VERSION);
			jCommander.usage();
			return;
		}

		Validator validator = command.validate();
		if (!validator.isValid()) {
			handleInvalidCommandLine(jCommander, StringUtils.CR + validator.getErrorMessage());
		}

		logger.info("Starting CQSE JaCoCo agent " + VERSION + " compiled against JaCoCo " + JaCoCo.VERSION);
		command.run();
	}

	/** Creates a builder for a {@link JCommander} object. */
	private Builder createJCommanderBuilder() {
		return JCommander.newBuilder().programName(Main.class.getName()).addObject(defaultArguments).addObject(command);
	}

	/** Shows an informative error and help message. Then exits the program. */
	private static void handleInvalidCommandLine(JCommander jCommander, String message) {
		System.err.println("Invalid command line: " + message + StringUtils.CR);
		jCommander.usage();
		System.exit(1);
	}

	/** Default arguments that may always be provided. */
	private static class DefaultArguments {

		/** Shows the help message. */
		@Parameter(names = "--help", help = true, description = "Shows all available command line arguments.")
		private boolean help;

	}

}
