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

/** Collects test details for all tests that match the given options. */
public class TestDetailsCollector {

	/** The logger. */
	private final Logger logger;

	/** A {@link Launcher} factory. */
	private final Supplier<Launcher> launcherSupplier;

	/** Constructor. */
	public TestDetailsCollector(Logger logger) {
		this.logger = logger;
		this.launcherSupplier = LauncherFactory::create;
	}

	public List<TestDetails> collect(ImpactedTestsExecutorCommandLineOptions options) {
		TestPlan fullTestPlan = buildTestPlan(options);
		return retrieveTestDetailsFromTestPlan(fullTestPlan, logger);
	}

	/**
	 * Starts test discovery for the given options.
	 *
	 * @return Returns a test plan.
	 */
	private TestPlan buildTestPlan(ImpactedTestsExecutorCommandLineOptions options) {
		Launcher launcher = launcherSupplier.get();
		LauncherDiscoveryRequest discoveryRequest = new DiscoveryRequestCreator()
				.toDiscoveryRequest(options.toJUnitOptions());
		return launcher.discover(discoveryRequest);
	}

	/** Extracts the test details from the JUnit test plan. */
	public static List<TestDetails> retrieveTestDetailsFromTestPlan(TestPlan fullTestPlan, Logger logger) {
		Set<TestIdentifier> roots = fullTestPlan.getRoots();
		ArrayList<TestDetails> allAvailableTestDetails = new ArrayList<>();
		collectTestDetailsList(fullTestPlan, roots, allAvailableTestDetails, logger);
		return allAvailableTestDetails;
	}

	/** Recursively traverses the test plan to collect all test details in a depth-first-search manner. */
	private static void collectTestDetailsList(TestPlan testPlan, Set<TestIdentifier> roots, List<TestDetails> result, Logger logger) {
		for (TestIdentifier testIdentifier : roots) {
			if (testIdentifier.isTest()) {
				Optional<TestSource> source = testIdentifier.getSource();
				if (source.isPresent() && source.get() instanceof MethodSource) {
					MethodSource ms = (MethodSource) source.get();
					String sourcePath = ms.getClassName().replace('.', '/');

					String testUniformPath = TestIdentifierUtils.getTestUniformPath(testIdentifier, logger);
					result.add(new TestDetails(testUniformPath, sourcePath, null));
				}
			}

			collectTestDetailsList(testPlan, testPlan.getChildren(testIdentifier), result, logger);
		}
	}

}