package com.teamscale.jacoco.agent

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.teamscale.jacoco.agent.convert.ConvertCommand
import com.teamscale.jacoco.agent.util.AgentUtils
import com.teamscale.jacoco.agent.util.LoggingUtils
import org.conqat.lib.commons.string.StringUtils
import org.jacoco.core.JaCoCo
import org.slf4j.Logger
import kotlin.system.exitProcess

/** Provides a command line interface for interacting with JaCoCo.  */
object Main {
	/** The logger.  */
	private val logger: Logger = LoggingUtils.getLogger(this)

	/** The default arguments that will always be parsed.  */
	private val defaultArguments = DefaultArguments()

	/** The arguments for the one-time conversion process.  */
	private val command = ConvertCommand()

	/** Entry point.  */
	@Throws(Exception::class)
	@JvmStatic
	fun main(args: Array<String>) {
		parseCommandLineAndRun(args)
	}

	/** Shows an informative error and help message. Then exits the program.  */
	private fun handleInvalidCommandLine(jCommander: JCommander, message: String?) {
		System.err.println("Invalid command line: " + message + StringUtils.LINE_FEED)
		jCommander.usage()
		exitProcess(1)
	}

	/**
	 * Parses the given command line arguments. Exits the program or throws an
	 * exception if the arguments are not valid. Then runs the specified command.
	 */
	@Throws(Exception::class)
	private fun parseCommandLineAndRun(args: Array<String>) {
		val builder = createJCommanderBuilder()
		val jCommander = builder.build()

		try {
			jCommander.parse(*args)
		} catch (e: ParameterException) {
			handleInvalidCommandLine(jCommander, e.message)
		}

		if (defaultArguments.help) {
			println("CQSE JaCoCo agent " + AgentUtils.VERSION + " compiled against JaCoCo " + JaCoCo.VERSION)
			jCommander.usage()
			return
		}

		val validator = command.validate()
		if (!validator.isValid) {
			handleInvalidCommandLine(jCommander, StringUtils.LINE_FEED + validator.errorMessage)
		}

		logger.info("Starting CQSE JaCoCo agent " + AgentUtils.VERSION + " compiled against JaCoCo " + JaCoCo.VERSION)
		command.run()
	}

	/** Creates a builder for a [JCommander] object.  */
	private fun createJCommanderBuilder() =
		JCommander.newBuilder().programName(Main::class.java.name)
			.addObject(defaultArguments).addObject(command)

	/** Default arguments that may always be provided.  */
	private class DefaultArguments {
		/** Shows the help message.  */
		@Parameter(names = ["--help"], help = true, description = "Shows all available command line arguments.")
		val help: Boolean = false
	}
}
