package com.teamscale.profiler.installer;

/**
 * Utilities for interacting with stdout, stderr and terminating the program due
 * to fatal errors.
 */
public class LogUtils {

	private static boolean printStackTracesForKnownErrors = false;
	private static boolean debugLogEnabled = false;

	/**
	 * Enables printing stack traces even when the error is known and explicitly
	 * handled. Useful for debugging incorrect error handling.
	 */
	public static void enableStackTracePrintingForKnownErrors() {
		printStackTracesForKnownErrors = true;
	}

	/**
	 * Print the stack trace of the throwable as debug info, then print the error
	 * message and exit the program.
	 * <p>
	 * Use this function only for unexpected errors where we definitely want the
	 * user to report the stack trace. For errors that the program can handle and
	 * where the stack trace is usually just noise we don't care about, please use
	 * {@link #failWithoutStackTrace(String, Throwable)} instead.
	 */
	public static void failWithStackTrace(Throwable throwable, String message) {
		throwable.printStackTrace();
		fail(message + "\nThis is a bug. Please report it to CQSE (support@teamscale.com).");
	}

	/**
	 * Print the error message for a known and handled error and exit the program.
	 * <p>
	 * If {@link #enableStackTracePrintingForKnownErrors()} was called, also prints
	 * the stack trace of the given throwable.
	 */
	public static void failWithoutStackTrace(String message, Throwable throwable) {
		if (printStackTracesForKnownErrors) {
			throwable.printStackTrace();
		} else {
			System.err.println("ERROR: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
			System.err.println(
					"Stack trace suppressed. Rerun this command with --stacktrace to see the stack trace.");
		}
		fail(message);
	}

	/**
	 * Print error message and exit the program.
	 */
	public static void fail(String message) {
		System.err.println();
		System.err.println(message);
		System.exit(1);
	}

	/**
	 * Print a warning message to stderr.
	 */
	public static void warn(String message) {
		System.err.println("WARNING: " + message);
	}

	/**
	 * Print a warning message to stderr and log the given throwable.
	 */
	public static void warn(String message, Throwable throwable) {
		warn(message);
		throwable.printStackTrace();
	}

	/**
	 * Print an info message to stdout.
	 * <p>
	 * Use sparingly and only if the information is helpful and actionable to the
	 * user. Logging too many implementation details is confusing to the user and
	 * may lead to them skipping over important information. CLI output should be
	 * concise.
	 * <p>
	 * Use {@link #debug(String)} instead for implementation details and information
	 * that is only helpful when debugging unforseen errors.
	 */
	public static void info(String message) {
		System.out.println("INFO: " + message);
	}

	/**
	 * Print a debug message to stdout.
	 * <p>
	 * Use to log information that is not useful during normal operations but
	 * helpful when something goes wrong.
	 */
	public static void debug(String message) {
		if (debugLogEnabled) {
			System.out.println("DEBUG: " + message);
		}
	}

	/**
	 * Enables debug logging and all stack traces.
	 */
	public static void enableDebugLogging() {
		debugLogEnabled = true;
		printStackTracesForKnownErrors = true;
	}
}
