/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.tasks;

import eu.cqse.teamscale.test.listeners.JUnit5TestListenerExtension;
import org.junit.platform.console.Logger;
import org.junit.platform.console.options.ImpactedTestsExecutorCommandLineOptions;
import org.junit.platform.console.options.Details;
import org.junit.platform.console.options.Theme;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.platform.console.tasks.ConsoleInterceptor.ignoreOut;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * Runs a set of given tests.
 */
public class TestExecutor {

	/** The logger. */
	private final Logger logger;

	/** The command line options. */
	private final ImpactedTestsExecutorCommandLineOptions options;

	/** A {@link Launcher} factory. */
	private final Supplier<Launcher> launcherSupplier;

	/** Constructor. */
	public TestExecutor(ImpactedTestsExecutorCommandLineOptions options, Logger logger) {
		this.options = options;
		this.logger = logger;
		this.launcherSupplier = LauncherFactory::create;
	}

	/** Executes the given list of tests. */
	public TestExecutionSummary executeTests(List<String> tests) {
		logger.message("Executing " + tests.size() + " impacted tests...");
		return executeRequest(generateImpactedDiscoveryRequest(tests));
	}

	/** Executes all tests included in {@link #options}. */
	public TestExecutionSummary executeAllTests() {
		LauncherDiscoveryRequest result;
		logger.message("Executing all tests...");
		result = new DiscoveryRequestCreator().toDiscoveryRequest(options.toJUnitOptions());
		LauncherDiscoveryRequest discoveryRequest = result;

		return executeRequest(discoveryRequest);
	}

	private TestExecutionSummary executeRequest(LauncherDiscoveryRequest discoveryRequest) {
		Launcher launcher = launcherSupplier.get();
		SummaryGeneratingListener summaryListener = registerListeners(launcher);
		ignoreOut(() -> launcher.execute(discoveryRequest));

		TestExecutionSummary summary = summaryListener.getSummary();
		if (summary.getTotalFailureCount() > 0 || options.getDetails() != Details.NONE) {
			printSummary(summary);
		}

		return summary;
	}

	private LauncherDiscoveryRequest generateImpactedDiscoveryRequest(List<String> tests) {
		List<DiscoverySelector> discoverySelectors = new ArrayList<>();
		for (String impactedTestCase : tests) {
			discoverySelectors.add(DiscoverySelectors.selectUniqueId(impactedTestCase));
		}
		return request().selectors(discoverySelectors).build();
	}

	private SummaryGeneratingListener registerListeners(Launcher launcher) {
		// always register summary generating listener
		SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(summaryListener);
		// Add jacoco aware test execution listener
		JUnit5TestListenerExtension jacocoListener = new JUnit5TestListenerExtension();
		launcher.registerTestExecutionListeners(jacocoListener);
		// optionally, register test plan execution details printing listener
		createDetailsPrintingListener(logger.out).ifPresent(launcher::registerTestExecutionListeners);
		// optionally, register XML reports writing listener
		createXmlWritingListener(logger.out).ifPresent(launcher::registerTestExecutionListeners);
		return summaryListener;
	}

	private Optional<TestExecutionListener> createDetailsPrintingListener(PrintWriter out) {
		boolean disableAnsiColors = options.isAnsiColorOutputDisabled();
		Theme theme = options.getTheme();
		switch (options.getDetails()) {
			case SUMMARY:
				// summary listener is always created and registered
				return Optional.empty();
			case FLAT:
				return Optional.of(new FlatPrintingListener(out, disableAnsiColors));
			case TREE:
				return Optional.of(new TreePrintingListener(out, disableAnsiColors, theme));
			case VERBOSE:
				return Optional.of(new VerboseTreePrintingListener(out, disableAnsiColors, 16, theme));
			default:
				return Optional.empty();
		}
	}

	private Optional<TestExecutionListener> createXmlWritingListener(PrintWriter out) {
		return options.getReportsDir().map(reportsDir -> new XmlReportsWritingListener(reportsDir, out));
	}

	private void printSummary(TestExecutionSummary summary) {
		// Otherwise the failures have already been printed in detail
		if (EnumSet.of(Details.NONE, Details.SUMMARY, Details.TREE).contains(options.getDetails())) {
			summary.printFailuresTo(logger.err);
		}
		summary.printTo(logger.out);
	}

}