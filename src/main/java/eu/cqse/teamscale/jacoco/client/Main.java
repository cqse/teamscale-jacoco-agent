package eu.cqse.teamscale.jacoco.client;

import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.JaCoCo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.ParameterException;

import eu.cqse.teamscale.jacoco.client.commandline.ECommand;
import eu.cqse.teamscale.jacoco.client.commandline.ICommand;
import eu.cqse.teamscale.jacoco.client.commandline.Validator;

/**
 * Connects to a running JaCoCo and regularly dumps coverage to XML files on
 * disk.
 */
public class Main {

	/** Version of this program. */
	private static final String VERSION;

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("eu.cqse.teamscale.jacoco.client.app");
		VERSION = bundle.getString("version");
	}

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Entry point. */
	public static void main(String[] args) throws Exception {
		new Main().parseCommandLineAndRun(args);
	}

	/**
	 * Parses the given command line arguments. Exits the program or throws an
	 * exception if the arguments are not valid. Then runs the specified command.
	 */
	private void parseCommandLineAndRun(String[] args) throws Exception {
		Builder builder = JCommander.newBuilder().programName(Main.class.getName());
		for (ECommand command : ECommand.values()) {
			builder.addCommand(command.implementation);
		}

		JCommander jCommander = builder.build();
		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			handleInvalidCommandLine(jCommander, e.getMessage());
		}

		ECommand commandType = ECommand.from(jCommander);
		ICommand command = commandType.implementation;
		Validator validator = command.validate();
		if (!validator.isValid()) {
			handleInvalidCommandLine(jCommander, StringUtils.CR + validator.getErrorMessage());
		}

		logger.info("Starting CQSE JaCoCo client " + VERSION + " compiled against JaCoCo " + JaCoCo.VERSION
				+ " with command " + commandType);
		command.run();
	}

	/** Shows an informative error and help message. Then exits the program. */
	private static void handleInvalidCommandLine(JCommander jCommander, String message) {
		System.err.println("Invalid command line: " + message);
		jCommander.usage();
		System.exit(1);
	}

}
