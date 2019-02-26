/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.commandline;

/**
 * Interface for commands: argument parsing and execution.
 */
public interface ICommand {

	/**
	 * Makes sure the arguments are valid. Must return all detected problems in the
	 * form of a user-visible message.
	 */
	Validator validate();

	/**
	 * Runs the implementation of the command. May throw an exception to indicate
	 * abnormal termination of the program.
	 */
	void run() throws Exception;

}