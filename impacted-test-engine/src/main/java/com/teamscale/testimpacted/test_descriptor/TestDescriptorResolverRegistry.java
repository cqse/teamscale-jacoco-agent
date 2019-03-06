package com.teamscale.testimpacted.test_descriptor;

import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.engine.TestEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry containing the default and custom {@link ITestDescriptorResolver}s discovered by the java {@link
 * ServiceLoader}.
 */
public class TestDescriptorResolverRegistry {

	private static final Map<String, ITestDescriptorResolver> TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID = new HashMap<>();

	static {
		// Register default test descriptor resolvers
		registerTestDescriptorResolver(new JUnitJupiterTestDescriptorResolver());
		registerTestDescriptorResolver(new JUnitVintageTestDescriptorResolver());

		// Override or register new test descriptor resolvers
		for (ITestDescriptorResolver testDescriptorResolver : ServiceLoader
				.load(ITestDescriptorResolver.class, ClassLoaderUtils.getDefaultClassLoader())) {
			registerTestDescriptorResolver(testDescriptorResolver);
		}
	}

	private static void registerTestDescriptorResolver(ITestDescriptorResolver testDescriptorResolver) {
		TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.put(testDescriptorResolver.getEngineId(), testDescriptorResolver);
	}

	/** Returns the test descriptor resolver or null if none exists for the test engine. */
	public static ITestDescriptorResolver getTestDescriptorResolver(TestEngine testEngine) {
		return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.get(testEngine.getId());
	}

}
