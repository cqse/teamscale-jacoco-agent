package org.junit.platform.console.options;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.console.shadow.joptsimple.BuiltinHelpFormatter;
import org.junit.platform.console.shadow.joptsimple.OptionDescriptor;
import org.junit.platform.console.shadow.joptsimple.OptionException;
import org.junit.platform.console.shadow.joptsimple.OptionParser;
import org.junit.platform.console.shadow.joptsimple.OptionSet;

public class TestExecutorCommandLineOptionsParser {

	public ImpactedTestsExecutorCommandLineOptions parse(String... arguments) {
		AvailableImpactedTestsExecutorCommandLineOptions availableOptions = getAvailableOptions();
		OptionParser parser = availableOptions.getParser();
		try {
			OptionSet detectedOptions = parser.parse(arguments);
			return availableOptions.toCustomCommandLineOptions(detectedOptions);
		} catch (OptionException e) {
			throw new JUnitException("Error parsing command-line arguments", e);
		}
	}

	public void printHelp(PrintWriter writer) {
		OptionParser optionParser = getAvailableOptions().getParser();
		optionParser.formatHelpWith(new OrderPreservingHelpFormatter());
		try {
			optionParser.printHelpOn(writer);
		} catch (IOException e) {
			throw new JUnitException("Error printing help", e);
		}
	}

	private AvailableImpactedTestsExecutorCommandLineOptions getAvailableOptions() {
		return new AvailableImpactedTestsExecutorCommandLineOptions();
	}

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
