package com.teamscale.testimpacted.test_descriptor;

import org.junit.platform.engine.TestEngine;

import java.util.HashMap;
import java.util.Map;

public class TestDescriptorResolverFactory {

	private static final Map<String, ITestDescriptorResolver> TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID = new HashMap<>();

	static {
		TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.put("junit-jupiter", new JUnitJupiterTestDescriptorResolver());
		TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.put("junit-vintage", new JUnitVintageTestDescriptorResolver());
	}

	public static ITestDescriptorResolver getTestDescriptorResolver(String testEngineId) {
		return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.get(testEngineId);
	}

	public static ITestDescriptorResolver getTestDescriptorResolver(TestEngine testEngine) {
		return getTestDescriptorResolver(testEngine.getId());
	}

}
