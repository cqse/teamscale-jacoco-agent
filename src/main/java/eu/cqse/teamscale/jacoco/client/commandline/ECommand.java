/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.commandline;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.enums.EnumUtils;
import org.conqat.lib.commons.string.StringUtils;

import com.beust.jcommander.JCommander;

import eu.cqse.teamscale.jacoco.client.convert.ConvertCommand;
import eu.cqse.teamscale.jacoco.client.watch.WatchCommand;

/**
 * All commands available on the command line.
 */
public enum ECommand {

	/** Continuously watches and dumps coverage from a running JaCoCo TCP port. */
	WATCH(new WatchCommand()),

	/** Does a one-time conversion from .exec to XML. */
	CONVERT(new ConvertCommand());

	/** The default command to use when no command is given. */
	public static final ECommand DEFAULT_COMMAND = WATCH;

	/** The arguments object for parsing command line arguments. */
	public final ICommand implementation;

	/** Constructor. */
	private ECommand(ICommand implementation) {
		this.implementation = implementation;
	}

	/**
	 * Returns the command the user entered or throws an {@link AssertionError}.
	 */
	public static ECommand from(JCommander jCommander) {
		String commandName = jCommander.getParsedCommand();
		if (StringUtils.isEmpty(commandName)) {
			return ECommand.DEFAULT_COMMAND;
		}

		ECommand command = EnumUtils.valueOfIgnoreCase(ECommand.class, commandName);
		CCSMAssert.isNotNull(command, "Unknown command " + commandName);
		return command;
	}

}
