package org.junit.platform.console;

import eu.cqse.teamscale.report.util.ILogger;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/** Logger to print to a given out and error stream. */
public class Logger implements AutoCloseable, ILogger {

	/** ANSI escape sequence for writing red text to the console. */
	private static final String ANSI_RED = "\u001B[31m";

	/** ANSI escape sequence for resetting any coloring options. */
	private static final String ANSI_RESET = "\u001B[0m";

	/** The normal output writer. */
	public final PrintWriter output;

	/** The error output writer. */
	public final PrintWriter error;

	/** Whether to print errors in red. */
	private boolean ansiColorEnabled;

	/** Constructor. */
	public Logger(PrintStream outStream, PrintStream errorStream) {
		output = new PrintWriter(new OutputStreamWriter(outStream, Charset.forName("UTF-8")), true);
		error = new PrintWriter(new OutputStreamWriter(errorStream, Charset.forName("UTF-8")), true);
	}

	/** @see #ansiColorEnabled */
	public void setAnsiColorEnabled(boolean ansiColorEnabled) {
		this.ansiColorEnabled = ansiColorEnabled;
	}

	/** Write the given exception to the error stream (stacktrace inclusive). */
	@Override
	public void error(Throwable exception) {
		exception.printStackTrace(error);
	}

	/** Prints the given info to the error stream. */
	public void error(String message) {
		if (ansiColorEnabled) {
			error.println(ANSI_RED + message + ANSI_RESET);
		} else {
			error.println(message);
		}
	}

	/** Prints the given info to the out stream. */
	@Override
	public void info(String message) {
		output.println(message);
	}

	@Override
	public void close() {
		error.close();
		output.close();
	}

	@Override
	public void debug(String s) {
		output.println(s);
	}

	@Override
	public void warn(String s) {
		output.println("WARN: " + s);
	}

	@Override
	public void warn(String s, Throwable throwable) {
		output.println("WARN: " + s);
		throwable.printStackTrace(output);
	}

	@Override
	public void error(String s, Throwable throwable) {
		error.println(s);
		throwable.printStackTrace(error);
	}
}
