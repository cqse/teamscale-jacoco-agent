package com.teamscale.test_impacted.test_descriptor

import com.teamscale.test_impacted.commons.LoggerUtils.getLogger
import org.junit.platform.commons.util.ClassLoaderUtils
import java.util.*

/**
 * Registry containing the default and custom [ITestDescriptorResolver]s discovered by the java
 * [ServiceLoader].
 */
object TestDescriptorResolverRegistry {
	private val LOGGER = getLogger(TestDescriptorResolverRegistry::class.java)

	private val TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID = mutableMapOf<String, ITestDescriptorResolver>()

	init {
		// Register default test descriptor resolvers
		registerTestDescriptorResolver(JUnitJupiterTestDescriptorResolver())
		registerTestDescriptorResolver(JUnitVintageTestDescriptorResolver())
		registerTestDescriptorResolver(JUnitPlatformSuiteDescriptorResolver())
		registerTestDescriptorResolver(CucumberPickleDescriptorResolver())

		// Override existing or register new test descriptor resolvers
		for (testDescriptorResolver in ServiceLoader
			.load(ITestDescriptorResolver::class.java, ClassLoaderUtils.getDefaultClassLoader())) {
			registerTestDescriptorResolver(testDescriptorResolver)
		}
	}

	private fun registerTestDescriptorResolver(testDescriptorResolver: ITestDescriptorResolver) {
		TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID[testDescriptorResolver.engineId] =
			testDescriptorResolver
	}

	/** Returns the test descriptor resolver or null if none exists for the test engine.  */
	@JvmStatic
	fun getTestDescriptorResolver(testEngineId: String): ITestDescriptorResolver? {
		if (!TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID.containsKey(testEngineId)) {
			LOGGER.warning {
				testEngineId + " is not officially supported! You can add support by " +
						"implementing the ITestDescriptorResolver interface and making the implementation " +
						"discoverable via the Java Service Loader mechanism!"
			}
			return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID["junit-jupiter"]
		}
		return TEST_DESCRIPTOR_RESOLVER_BY_ENGINE_ID[testEngineId]
	}
}
