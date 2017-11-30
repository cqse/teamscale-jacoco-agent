/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.commandline;

import java.io.IOException;

/**
 * Interface for commands: argument parsing and execution.
 */
public interface ICommand {

	/** Makes sure the arguments are valid. */
	void validate() throws IOException, AssertionError;

	/**
	 * Runs the implementation of the command. May throw an exception to indicate
	 * abnormal termination of the program.
	 */
	void run() throws Exception;

}