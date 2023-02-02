package com.teamscale.test_impacted.test_descriptor;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.engine.TestEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry containing the default and custom {@link ITestDescriptorResolver}s discovered by the java
 * {@link ServiceLoader}.
 */
public class TestDescriptorResolverRegistry {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDescriptorResolverRegistry.class);

	private static final Map<String, ITestDescriptorResolver> TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID = new HashMap<>();

	static {
		// Register default test descriptor resolvers
		registerTestDescriptorResolver(new JUnitJupiterTestDescriptorResolver());
		registerTestDescriptorResolver(new JUnitVintageTestDescriptorResolver());

		// Override existing or register new test descriptor resolvers
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
		String testEngineId = testEngine.getId();
		if (!TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.containsKey(testEngineId)) {
			LOGGER.warn(() -> testEngineId + " is not officially supported! You can add support by " +
					"implementing the ITestDescriptorResolver interface and making the implementation via the Java Service Loader mechanism!");
			return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.get("junit-jupiter");
		}
		return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.get(testEngineId);
	}

}
