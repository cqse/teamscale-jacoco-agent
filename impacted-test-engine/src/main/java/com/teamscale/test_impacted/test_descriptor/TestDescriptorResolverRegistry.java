package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.commons.util.ClassLoaderUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Registry containing the default and custom {@link ITestDescriptorResolver}s discovered by the java
 * {@link ServiceLoader}.
 */
public class TestDescriptorResolverRegistry {

	private static final Logger LOGGER = LoggerUtils.getLogger(TestDescriptorResolverRegistry.class);

	private static final Map<String, ITestDescriptorResolver> TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID = new HashMap<>();

	static {
		// Register default test descriptor resolvers
		registerTestDescriptorResolver(new JUnitJupiterTestDescriptorResolver());
		registerTestDescriptorResolver(new JUnitVintageTestDescriptorResolver());
		registerTestDescriptorResolver(new JUnitPlatformSuiteDescriptorResolver());
		registerTestDescriptorResolver(new CucumberPickleDescriptorResolver());

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
	public static ITestDescriptorResolver getTestDescriptorResolver(String testEngineId) {
		if (!TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.containsKey(testEngineId)) {
			LOGGER.warning(() -> testEngineId + " is not officially supported! You can add support by " +
					"implementing the ITestDescriptorResolver interface and making the implementation " +
					"discoverable via the Java Service Loader mechanism!");
			return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.get("junit-jupiter");
		}
		return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.get(testEngineId);
	}

}
