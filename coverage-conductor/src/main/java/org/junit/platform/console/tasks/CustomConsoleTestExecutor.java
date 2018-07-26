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
import org.apiguardian.api.API;
import org.junit.platform.console.options.CustomCommandLineOptions;
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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.console.tasks.ConsoleInterceptor.ignoreOut;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public class CustomConsoleTestExecutor extends CustomTestExecutorBase {

	private final List<String> tests;

	public CustomConsoleTestExecutor(CustomCommandLineOptions options, List<String> tests) {
		this(options, LauncherFactory::create, tests);
	}

	// for tests only
	private CustomConsoleTestExecutor(CustomCommandLineOptions options, Supplier<Launcher> launcherSupplier, List<String> tests) {
		super(options, launcherSupplier);
		this.tests = tests;
	}

	public TestExecutionSummary execute(PrintWriter out) throws Exception {
		return new CustomContextClassLoaderExecutor(createCustomClassLoader()).invoke(() -> executeTests(out));
	}

	private TestExecutionSummary executeTests(PrintWriter out) {
		Launcher launcher = launcherSupplier.get();

		LauncherDiscoveryRequest discoveryRequest;
		if (tests == null) {
			System.out.println("Executing all tests...");
			discoveryRequest = new DiscoveryRequestCreator().toDiscoveryRequest(options.toJUnitOptions());
		} else {
			System.out.println("Executing " + tests.size() + " impacted tests...");
			discoveryRequest = generateImpactedDiscoveryRequest();
		}

		SummaryGeneratingListener summaryListener = registerListeners(out, launcher);
		ignoreOut(() -> launcher.execute(discoveryRequest));

		TestExecutionSummary summary = summaryListener.getSummary();
		if (summary.getTotalFailureCount() > 0 || options.getDetails() != Details.NONE) {
			printSummary(summary, out);
		}

		return summary;
	}

	private LauncherDiscoveryRequest generateImpactedDiscoveryRequest() {
		List<DiscoverySelector> discoverySelectors = new ArrayList<>();
		for (String impactedTestCase : tests) {
			discoverySelectors.add(DiscoverySelectors.selectUniqueId(impactedTestCase));
		}
		return request().selectors(discoverySelectors).build();
	}

	private SummaryGeneratingListener registerListeners(PrintWriter out, Launcher launcher) {
		// always register summary generating listener
		SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(summaryListener);
		// Add jacoco aware test execution listener
		JUnit5TestListenerExtension jacocoListener = new JUnit5TestListenerExtension();
		launcher.registerTestExecutionListeners(jacocoListener);
		// optionally, register test plan execution details printing listener
		createDetailsPrintingListener(out).ifPresent(launcher::registerTestExecutionListeners);
		// optionally, register XML reports writing listener
		createXmlWritingListener(out).ifPresent(launcher::registerTestExecutionListeners);
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

	private void printSummary(TestExecutionSummary summary, PrintWriter out) {
		// Otherwise the failures have already been printed in detail
		if (EnumSet.of(Details.NONE, Details.SUMMARY, Details.TREE).contains(options.getDetails())) {
			summary.printFailuresTo(out);
		}
		summary.printTo(out);
	}

}