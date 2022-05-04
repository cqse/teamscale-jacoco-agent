package com.teamscale.maven.surefire;


import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Surefire provider for Teamscale's impacted test engine.
 * <p>
 * This package contains a copy of the {@link JUnitPlatformProvider} (and other required classes) from
 * https://github.com/apache/maven-surefire/tree/surefire-3.0.0-M5/surefire-providers/surefire-junit-platform. This
 * subclass disables all other test engines except our own.
 * <p>
 * Ideally, we would like to subclass the original {@link JUnitPlatformProvider} and only override what we want to
 * change, but it is not published as an artifact by Surefire and all the stuff we want to modify is private. So we
 * copied the {@link JUnitPlatformProvider} class and made the necessary extension point protected ({@link
 * #newFilters()}).
 * <p>
 * Starting with Maven 3.0.0-M6, users can also configure the included engines in Surefire directly via {@code
 * includeJUnit5Engines}. However, that has not yet been released.
 * <p>
 * Copyright notices in the files in this project must be retained, as the files are copies from the Maven Surefire
 * repository.
 */
public class ImpactedTestEngineProvider
		extends JUnitPlatformProvider {

	public ImpactedTestEngineProvider(ProviderParameters parameters) {
		super(parameters);
	}

	ImpactedTestEngineProvider(ProviderParameters parameters, Launcher launcher) {
		super(parameters, launcher);
	}

	/**
	 * Extends the original {@link JUnitPlatformProvider#newFilters()} method by adding an {@link EngineFilter} for our
	 * impacted test engine. This causes only our engine to be executed.
	 */
	protected Filter<?>[] newFilters() {
		Filter<?>[] originalFilters = super.newFilters();
		List<Filter<?>> filters = new ArrayList<>(Arrays.asList(originalFilters));
		filters.add(EngineFilter.includeEngines("teamscale-test-impacted"));
		return filters.toArray(new Filter<?>[0]);
	}

}
