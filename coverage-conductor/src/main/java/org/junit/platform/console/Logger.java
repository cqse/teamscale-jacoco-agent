package org.junit.platform.console;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

/** Logger to print to a given out and error stream. */
public class Logger implements AutoCloseable {

	/** ANSI escape sequence for writing red text to the console. */
	private static final String ANSI_RED = "\u001B[31m";

	/** ANSI escape sequence for resetting any coloring options. */
	private static final String ANSI_RESET = "\u001B[0m";

	/** The normal output writer. */
	public final PrintWriter out;

	/** The error output writer. */
	public final PrintWriter error;

	/** Whether to print errors in red. */
	private boolean ansiColorEnabled;

	/** Constructor. */
	public Logger(PrintStream out, PrintStream error) {
		this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
		this.error = new PrintWriter(new BufferedWriter(new OutputStreamWriter(error)));
	}

	/** @see #ansiColorEnabled */
	public void setAnsiColorEnabled(boolean ansiColorEnabled) {
		this.ansiColorEnabled = ansiColorEnabled;
	}

	/** Write the given exception to the error stream (stacktrace inclusive). */
	public void error(Exception exception) {
		exception.printStackTrace(error);
		error.flush();
	}

	/** Prints the given message to the error stream. */
	public void error(String error) {
		if(ansiColorEnabled) {
			this.error.println(ANSI_RED + error + ANSI_RESET);
		} else {
			this.error.println(error);
		}
	}

	/** Prints the given message to the out stream. */
	public void message(String message) {
		out.println(message);
	}

	@Override
	public void close() {
		error.close();
		out.close();
	}
}
