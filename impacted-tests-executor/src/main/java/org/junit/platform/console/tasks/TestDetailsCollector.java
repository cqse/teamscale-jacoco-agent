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

import eu.cqse.teamscale.client.TestDetails;
import org.junit.platform.console.Logger;
import org.junit.platform.console.options.ImpactedTestsExecutorCommandLineOptions;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Collects test details for all tests that match the given options. */
public class TestDetailsCollector {

	/**
	 * Pattern that matches the classes fully qualified class name in JUnit's uniqueId as first capture group.
	 * The test's uniqueId is something similar to:
	 * [engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[method:testAdd()]
	 * [engine:junit-vintage]/[runner:com.example.project.JUnit4Test]/[test:testAdd(com.example.project.JUnit4Test)]
	 */
	private static final Pattern FULL_CLASS_NAME_PATTERN = Pattern.compile(".*\\[(?:class|runner):([^]]+)\\].*");

	/** The logger. */
	private final Logger logger;

	/** A {@link Launcher} factory. */
	private final Supplier<Launcher> launcherSupplier;

	/** Constructor. */
	public TestDetailsCollector(Logger logger) {
		this.logger = logger;
		this.launcherSupplier = LauncherFactory::create;
	}

	/**
	 * Starts collecting the test details for the given options.
	 *
	 * @return Returns a list with all test details or null if an unexpected error occurred.
	 */
	public List<TestDetails> collect(ImpactedTestsExecutorCommandLineOptions options) {
		Launcher launcher = launcherSupplier.get();
		LauncherDiscoveryRequest discoveryRequest = new DiscoveryRequestCreator()
				.toDiscoveryRequest(options.toJUnitOptions());
		TestPlan fullTestPlan = launcher.discover(discoveryRequest);
		return retrieveTestDetailsFromTestPlan(fullTestPlan);
	}

	/** Extracts the test details from the JUnit test plan. */
	private List<TestDetails> retrieveTestDetailsFromTestPlan(TestPlan fullTestPlan) {
		Set<TestIdentifier> roots = fullTestPlan.getRoots();
		ArrayList<TestDetails> allAvailableTestDetails = new ArrayList<>();
		collectTestDetailsList(fullTestPlan, roots, allAvailableTestDetails);
		return allAvailableTestDetails;
	}

	/** Recursively traverses the test plan to collect all test details in a depth-first-search manner. */
	private void collectTestDetailsList(TestPlan testPlan, Set<TestIdentifier> roots, List<TestDetails> result) {
		for (TestIdentifier testIdentifier : roots) {
			if (testIdentifier.isTest()) {
				Optional<TestSource> source = testIdentifier.getSource();
				if (source.isPresent() && source.get() instanceof MethodSource) {
					MethodSource ms = (MethodSource) source.get();
					String sourcePath = ms.getClassName().replace('.', '/');

					String uniqueId = testIdentifier.getUniqueId();
					String internalId = getTestInternalId(testIdentifier);
					String displayName = testIdentifier.getDisplayName();
					result.add(new TestDetails(uniqueId, internalId, sourcePath, displayName, null));
				}
			}

			collectTestDetailsList(testPlan, testPlan.getChildren(testIdentifier), result);
		}
	}

	/**
	 * Builds the internal ID which will later be displayed in Teamscale.
	 * We are using the legacy reporting name here, since this matches the format also used in the JUnit reports,
	 * which we need to map together in Teamscale.
	 */
	private String getTestInternalId(TestIdentifier testIdentifier) {
		return getFullyQualifiedClassName(testIdentifier) + '/' + testIdentifier.getLegacyReportingName();
	}

	/** Tries to extract the fully qualified class name from the given test identifier. */
	private String getFullyQualifiedClassName(TestIdentifier testIdentifier) {
		Matcher matcher = FULL_CLASS_NAME_PATTERN.matcher(testIdentifier.getUniqueId());
		String fullClassName = "unknown-class";
		if (!matcher.matches()) {
			logger.error("Unable to find class name for " + testIdentifier.getUniqueId());

			MethodSource ms = (MethodSource) testIdentifier.getSource().orElse(null);
			if (ms != null) {
				fullClassName = ms.getClassName();
			}
		} else {
			fullClassName = matcher.group(1);
		}
		return fullClassName.replace('.', '/');
	}
}