package org.apache.maven.surefire.teamscale;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_GROUPS_PROP;
import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.util.TestsToRun.fromClass;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * Surefire provider for Teamscale's impacted test engine.
 * <p>
 * This is a copy of the JUnitPlatformProvider. The only modification is {@link #addImpactedTestEngineFilter(List)} and
 * its usages. Basically, this provider disables all other test engines except our own.
 * <p>
 * Ideally, we would like to subclass the JUnitPlatformProvider and only override what we want to change, but it is not
 * published as an artifact by Surefire.
 * <p>
 * Starting with Maven 3.0.0-M6, users can also configure the included engines in Surefire directly via {@code
 * includeJUnit5Engines}. However, that has not yet been released.
 * <p>
 * Copyright notices in the files in this project must be retained, as the files are copies from the Maven Surefire
 * repository.
 */
public class ImpactedTestEngineProvider
		extends AbstractProvider {
	static final String CONFIGURATION_PARAMETERS = "configurationParameters";

	private final ProviderParameters parameters;

	private final Launcher launcher;

	private final Filter<?>[] filters;

	private final Map<String, String> configurationParameters;

	public ImpactedTestEngineProvider(ProviderParameters parameters) {
		this(parameters, LauncherFactory.create());
	}

	ImpactedTestEngineProvider(ProviderParameters parameters, Launcher launcher) {
		this.parameters = parameters;
		this.launcher = launcher;
		filters = newFilters();
		configurationParameters = newConfigurationParameters();
		Logger.getLogger("org.junit").setLevel(Level.WARNING);
	}

	@Override
	public Iterable<Class<?>> getSuites() {
		return scanClasspath();
	}

	@Override
	public RunResult invoke(Object forkTestSet)
			throws TestSetFailedException, ReporterException {
		ReporterFactory reporterFactory = parameters.getReporterFactory();
		final RunResult runResult;
		try {
			RunListener runListener = reporterFactory.createReporter();
			startCapture((ConsoleOutputReceiver) runListener);
			if (forkTestSet instanceof TestsToRun) {
				invokeAllTests((TestsToRun) forkTestSet, runListener);
			} else if (forkTestSet instanceof Class) {
				invokeAllTests(fromClass((Class<?>) forkTestSet), runListener);
			} else if (forkTestSet == null) {
				invokeAllTests(scanClasspath(), runListener);
			} else {
				throw new IllegalArgumentException(
						"Unexpected value of forkTestSet: " + forkTestSet);
			}
		} finally {
			runResult = reporterFactory.close();
		}
		return runResult;
	}

	private TestsToRun scanClasspath() {
		TestPlanScannerFilter filter = new TestPlanScannerFilter(launcher, filters);
		ScanResult scanResult = parameters.getScanResult();
		TestsToRun scannedClasses = scanResult.applyFilter(filter, parameters.getTestClassLoader());
		return parameters.getRunOrderCalculator().orderTestClasses(scannedClasses);
	}

	private void invokeAllTests(TestsToRun testsToRun, RunListener runListener) {
		RunListenerAdapter adapter = new RunListenerAdapter(runListener);
		execute(testsToRun, adapter);
		// Rerun failing tests if requested
		int count = parameters.getTestRequest().getRerunFailingTestsCount();
		if (count > 0 && adapter.hasFailingTests()) {
			for (int i = 0; i < count; i++) {
				// Replace the "discoveryRequest" so that it only specifies the failing tests
				LauncherDiscoveryRequest discoveryRequest = buildLauncherDiscoveryRequestForRerunFailures(adapter);
				// Reset adapter's recorded failures and invoke the failed tests again
				adapter.reset();
				launcher.execute(discoveryRequest, adapter);
				// If no tests fail in the rerun, we're done
				if (!adapter.hasFailingTests()) {
					break;
				}
			}
		}
	}

	private void execute(TestsToRun testsToRun, RunListenerAdapter adapter) {
		if (testsToRun.allowEagerReading()) {
			List<DiscoverySelector> selectors = new ArrayList<>();
			testsToRun.iterator()
					.forEachRemaining(c -> selectors.add(selectClass(c.getName())));

			LauncherDiscoveryRequestBuilder builder = request()
					.filters(filters)
					.configurationParameters(configurationParameters)
					.selectors(selectors);

			launcher.execute(builder.build(), adapter);
		} else {
			testsToRun.iterator()
					.forEachRemaining(c ->
					{
						LauncherDiscoveryRequestBuilder builder = request()
								.filters(filters)
								.configurationParameters(configurationParameters)
								.selectors(selectClass(c.getName()));
						launcher.execute(builder.build(), adapter);
					});
		}
	}

	private LauncherDiscoveryRequest buildLauncherDiscoveryRequestForRerunFailures(RunListenerAdapter adapter) {
		LauncherDiscoveryRequestBuilder builder = request().filters(filters).configurationParameters(
				configurationParameters);
		// Iterate over recorded failures
		for (TestIdentifier identifier : new LinkedHashSet<>(adapter.getFailures().keySet())) {
			builder.selectors(selectUniqueId(identifier.getUniqueId()));
		}
		return builder.build();
	}

	private Filter<?>[] newFilters() {
		List<Filter<?>> filters = new ArrayList<>();

		getPropertiesList(TESTNG_GROUPS_PROP)
				.map(TagFilter::includeTags)
				.ifPresent(filters::add);

		getPropertiesList(TESTNG_EXCLUDEDGROUPS_PROP)
				.map(TagFilter::excludeTags)
				.ifPresent(filters::add);

		addImpactedTestEngineFilter(filters);

		TestListResolver testListResolver = parameters.getTestRequest().getTestListResolver();
		if (!testListResolver.isEmpty()) {
			filters.add(new TestMethodFilter(testListResolver));
		}

		return filters.toArray(new Filter<?>[filters.size()]);
	}

	/** Adds a filter that disables all but our own test engine. */
	private void addImpactedTestEngineFilter(List<Filter<?>> filters) {
		filters.add(EngineFilter.includeEngines("teamscale-test-impacted"));
	}

	Filter<?>[] getFilters() {
		return filters;
	}

	private Map<String, String> newConfigurationParameters() {
		String content = parameters.getProviderProperties().get(CONFIGURATION_PARAMETERS);
		if (content == null) {
			return emptyMap();
		}
		try (StringReader reader = new StringReader(content)) {
			Map<String, String> result = new HashMap<>();
			Properties props = new Properties();
			props.load(reader);
			props.stringPropertyNames()
					.forEach(key -> result.put(key, props.getProperty(key)));
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException("Error reading " + CONFIGURATION_PARAMETERS, e);
		}
	}

	Map<String, String> getConfigurationParameters() {
		return configurationParameters;
	}

	private Optional<List<String>> getPropertiesList(String key) {
		String property = parameters.getProviderProperties().get(key);
		return isBlank(property) ? empty()
				: of(stream(property.split("[,]+"))
				.filter(StringUtils::isNotBlank)
				.map(String::trim)
				.collect(toList()));
	}
}