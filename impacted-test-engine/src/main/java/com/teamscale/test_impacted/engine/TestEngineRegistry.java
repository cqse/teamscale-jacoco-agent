package com.teamscale.test_impacted.engine;

import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.engine.TestEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/** The test engine registry containing all */
public class TestEngineRegistry implements Iterable<TestEngine> {

	private Map<String, TestEngine> testEnginesById;

	public TestEngineRegistry(Set<String> includedTestEngineIds, Set<String> excludedTestEngineIds) {
		List<TestEngine> otherTestEngines = loadOtherTestEngines(excludedTestEngineIds);

		// If there are no test engines set we don't need to filter but simply use all other test engines.
		if (!includedTestEngineIds.isEmpty()) {
			otherTestEngines = otherTestEngines.stream()
					.filter(testEngine -> includedTestEngineIds.contains(testEngine.getId())).collect(
							Collectors.toList());
		}

		testEnginesById = unmodifiableMap(otherTestEngines.stream().collect(toMap(TestEngine::getId, identity())));
	}

	/** Uses the {@link ServiceLoader} to discover all {@link TestEngine}s but the {@link ImpactedTestEngine}. */
	private List<TestEngine> loadOtherTestEngines(Set<String> excludedTestEngineIds) {
		List<TestEngine> testEngines = new ArrayList<>();

		for (TestEngine testEngine : ServiceLoader.load(TestEngine.class, ClassLoaderUtils.getDefaultClassLoader())) {
			if (!ImpactedTestEngine.ENGINE_ID.equals(testEngine.getId()) && !excludedTestEngineIds.contains(
					testEngine.getId())) {
				testEngines.add(testEngine);
			}
		}

		return testEngines;
	}

	/** Returns the {@link TestEngine} for the engine id or null if none is present. */
	public TestEngine getTestEngine(String engineId) {
		return testEnginesById.get(engineId);
	}

	@Override
	public Iterator<TestEngine> iterator() {
		List<TestEngine> testEngines = new ArrayList<>(testEnginesById.values());
		testEngines.sort(Comparator.comparing(TestEngine::getId));
		return testEngines.iterator();
	}
}
