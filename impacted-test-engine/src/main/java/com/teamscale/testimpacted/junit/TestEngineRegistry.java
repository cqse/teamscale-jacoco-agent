package com.teamscale.testimpacted.junit;

import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.engine.TestEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/** The test engine registry containing all */
class TestEngineRegistry implements Iterable<TestEngine> {

	private static final Set<String> TEST_ENGINE_IDS;

	static {
		TEST_ENGINE_IDS = getConfiguredTestEngineIds();
	}

	private Map<String, TestEngine> testEnginesById = null;

	private static Set<String> getConfiguredTestEngineIds() {
		String testEnginesProperty = System.getProperty("teamscale.test.impacted.engines");
		return Arrays.stream(testEnginesProperty.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	/**
	 * Utility method for lazily initializing the actual {@link TestEngine} test engines the {@link ImpactedTestEngine}
	 * delegates to. Must be lazy because of otherwise infinite recursion since the {@link ServiceLoader} will again
	 * discover the {@link ImpactedTestEngine} which references this {@link TestEngineRegistry}.
	 */
	private Map<String, TestEngine> getEnabledTestEngines() {
		if (testEnginesById != null) {
			return testEnginesById;
		}

		Map<String, TestEngine> collectedTestEnginesById = new HashMap<>();

		for (TestEngine testEngine : ServiceLoader.load(TestEngine.class, ClassLoaderUtils.getDefaultClassLoader())) {
			String testEngineId = testEngine.getId();

			if (ImpactedTestEngine.ENGINE_ID.equals(testEngineId)) {
				continue;
			}
			if (TEST_ENGINE_IDS.contains(testEngineId)) {
				collectedTestEnginesById.put(testEngineId, testEngine);
			}
		}

		testEnginesById = Collections.unmodifiableMap(collectedTestEnginesById);
		return testEnginesById;
	}

	/** Returns the {@link TestEngine} for the engine id or null if none is present. */
	TestEngine getTestEngine(String engineId) {
		return getEnabledTestEngines().get(engineId);
	}

	@Override
	public Iterator<TestEngine> iterator() {
		List<TestEngine> testEngines = new ArrayList<>(getEnabledTestEngines().values());
		testEngines.sort(Comparator.comparing(TestEngine::getId));
		return testEngines.iterator();
	}
}
