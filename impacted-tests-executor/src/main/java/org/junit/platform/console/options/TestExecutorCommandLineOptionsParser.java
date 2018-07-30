package org.junit.platform.console.options;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.console.shadow.joptsimple.BuiltinHelpFormatter;
import org.junit.platform.console.shadow.joptsimple.OptionDescriptor;
import org.junit.platform.console.shadow.joptsimple.OptionException;
import org.junit.platform.console.shadow.joptsimple.OptionParser;
import org.junit.platform.console.shadow.joptsimple.OptionSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Parser for command line options. It recognizes the standard jUnit
 * command line options as well as our own.
 */
public class TestExecutorCommandLineOptionsParser {

	/** Parses the given arguments. Throws a {@link JUnitException} in case of invalid input. */
	public ImpactedTestsExecutorCommandLineOptions parse(String... arguments) {
		AvailableImpactedTestsExecutorCommandLineOptions availableOptions = getAvailableOptions();
		OptionParser parser = availableOptions.getParser();
		try {
			OptionSet detectedOptions = parser.parse(arguments);
			return availableOptions.toCommandLineOptions(detectedOptions);
		} catch (OptionException e) {
			throw new JUnitException("Error parsing command-line arguments", e);
		}
	}

	/** Prints a summary of all available options to the given writer. */
	public void printHelp(PrintWriter writer) {
		OptionParser optionParser = getAvailableOptions().getParser();
		optionParser.formatHelpWith(new OrderPreservingHelpFormatter());
		try {
			optionParser.printHelpOn(writer);
		} catch (IOException e) {
			throw new JUnitException("Error printing help", e);
		}
	}

	/** Returns the available command line options. */
	private AvailableImpactedTestsExecutorCommandLineOptions getAvailableOptions() {
		return new AvailableImpactedTestsExecutorCommandLineOptions();
	}

	/** Helper class to print the options in the order of definition. */
	private static final class OrderPreservingHelpFormatter extends BuiltinHelpFormatter {

		private OrderPreservingHelpFormatter() {
			super(90, 4);
		}

		@Override
		public String format(Map<String, ? extends OptionDescriptor> options) {
			addRows(new LinkedHashSet<>(options.values()));
			return formattedHelpOutput();
		}
	}
}
