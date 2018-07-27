package org.junit.platform.console;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

public class Logger implements AutoCloseable {

	/** ANSI escape sequence for writing red text to the console. */
	private static final String ANSI_RED = "\u001B[31m";

	/** ANSI escape sequence for resetting any coloring options. */
	private static final String ANSI_RESET = "\u001B[0m";

	public final PrintWriter out;
	public final PrintWriter err;
	private boolean ansiColorEnabled;

	public Logger(PrintStream out, PrintStream err) {
		this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
		this.err = new PrintWriter(new BufferedWriter(new OutputStreamWriter(err)));
	}

	public void setAnsiColorEnabled(boolean ansiColorEnabled) {
		this.ansiColorEnabled = ansiColorEnabled;
	}

	public void error(Exception exception) {
		exception.printStackTrace(err);
		err.println();
		err.flush();
	}

	public void error(String error) {
		if(ansiColorEnabled) {
			err.println(ANSI_RED + error + ANSI_RESET);
		} else {
			err.println(error);
		}
	}

	public void message(String message) {
		out.println(message);
	}

	@Override
	public void close() {
		err.flush();
		err.close();
		out.flush();
		out.close();
	}
}
