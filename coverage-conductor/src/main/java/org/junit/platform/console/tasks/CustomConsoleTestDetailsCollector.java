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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.cqse.teamscale.client.TestDetails;
import org.apiguardian.api.API;
import org.junit.platform.console.options.CustomCommandLineOptions;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public class CustomConsoleTestDetailsCollector extends CustomTestExecutorBase {

	private final Pattern matcher = Pattern.compile(".*\\[(?:class|runner):([^]]+)\\].*");

	public CustomConsoleTestDetailsCollector(CustomCommandLineOptions options) {
		super(options, LauncherFactory::create);
	}

	public List<TestDetails> execute() throws Exception {
		return new CustomContextClassLoaderExecutor(createCustomClassLoader()).invoke(this::retrieveTestDetails);
	}

	private List<TestDetails> retrieveTestDetails() {
		Launcher launcher = launcherSupplier.get();
		LauncherDiscoveryRequest discoveryRequest = new DiscoveryRequestCreator().toDiscoveryRequest(options.toJUnitOptions());
		TestPlan fullTestPlan = launcher.discover(discoveryRequest);
		return retrieveTestDetailsFromTestPlan(fullTestPlan);
	}

	private List<TestDetails> retrieveTestDetailsFromTestPlan(TestPlan fullTestPlan) {
		Set<TestIdentifier> roots = fullTestPlan.getRoots();
		ArrayList<TestDetails> allAvailableTestDetails = new ArrayList<>();
		collectTestDetailsList(fullTestPlan, roots, allAvailableTestDetails);
		return allAvailableTestDetails;
	}

	private void collectTestDetailsList(TestPlan testPlan, Set<TestIdentifier> roots, List<TestDetails> result) {
		for (TestIdentifier testIdentifier: roots) {
			if (testIdentifier.getType() == TEST) {
				Optional<TestSource> source = testIdentifier.getSource();
				if (source.isPresent() && source.get() instanceof MethodSource) {
					MethodSource ms = (MethodSource) source.get();
					String sourcePath = ms.getClassName().replace('.', '/');

					String uniqueId = testIdentifier.getUniqueId();
					String uniformPath = getTestUniformPath(testIdentifier);
					String displayName = testIdentifier.getDisplayName();
					result.add(new TestDetails(uniqueId, uniformPath, sourcePath, displayName, ""));
				}
			}

			collectTestDetailsList(testPlan, testPlan.getChildren(testIdentifier), result);
		}
	}

	private String getTestUniformPath(TestIdentifier testIdentifier) {
		Matcher matcher = this.matcher.matcher(testIdentifier.getUniqueId());
		String fullClassName = "unknown-class";
		if (!matcher.matches()) {
			System.err.println("Unable to find class name for " + testIdentifier.getUniqueId());

			MethodSource ms = (MethodSource) testIdentifier.getSource().orElse(null);
			if (ms != null) {
				fullClassName = ms.getClassName();
			}
		} else {
			fullClassName = matcher.group(1);
		}
		return fullClassName.replace('.', '/') + '/' + testIdentifier.getLegacyReportingName();
	}
}