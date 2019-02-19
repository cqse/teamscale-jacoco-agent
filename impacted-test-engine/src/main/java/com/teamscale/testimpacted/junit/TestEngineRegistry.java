package com.teamscale.testimpacted.junit;

import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.engine.TestEngine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

class TestEngineRegistry {

	private static final Set<String> TEST_ENGINES;

	static {
		TEST_ENGINES = getConfiguredTestEngineIds();
	}

	private Map<String, TestEngine> testEnginesById = null;

	TestEngineRegistry() {
	}

	private static Set<String> getConfiguredTestEngineIds() {
		String testEnginesProperty = System.getProperty("teamscale.test.impacted.engines");
		return Arrays.stream(testEnginesProperty.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	Map<String, TestEngine> getEnabledTestEngines() {
		if (testEnginesById != null) {
			return testEnginesById;
		}

		Map<String, TestEngine> collectedTestEnginesById = new HashMap<>();

		for (TestEngine testEngine : ServiceLoader.load(TestEngine.class, ClassLoaderUtils.getDefaultClassLoader())) {
			String testEngineId = testEngine.getId();

			if (ImpactedTestEngine.ENGINE_ID.equals(testEngineId)) {
				continue;
			}
			if (TEST_ENGINES.contains(testEngineId)) {
				collectedTestEnginesById.put(testEngineId, testEngine);
			}
		}

		testEnginesById = Collections.unmodifiableMap(collectedTestEnginesById);
		return testEnginesById;
	}
}
