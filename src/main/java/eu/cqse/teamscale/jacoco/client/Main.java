package eu.cqse.teamscale.jacoco.client;

import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.JaCoCo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.ParameterException;

import eu.cqse.teamscale.jacoco.client.commandline.ECommand;
import eu.cqse.teamscale.jacoco.client.commandline.ICommand;
import eu.cqse.teamscale.jacoco.client.convert.ConvertCommand;
import eu.cqse.teamscale.jacoco.client.watch.WatchCommand;

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

		JCommander jCommander = builder.addCommand(new WatchCommand()).addCommand(new ConvertCommand()).build();
		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jCommander.usage();
			System.exit(1);
		}

		ICommand command = ECommand.from(jCommander).implementation;
		command.validate();

		logger.info("Starting JaCoCo client " + VERSION + " compiled against JaCoCo " + JaCoCo.VERSION);
		command.run();
	}

}
