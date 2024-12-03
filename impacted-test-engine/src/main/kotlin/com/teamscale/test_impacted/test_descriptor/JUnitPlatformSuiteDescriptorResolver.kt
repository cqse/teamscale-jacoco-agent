package com.teamscale.test_impacted.test_descriptor

import com.teamscale.test_impacted.commons.LoggerUtils.getLogger
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import java.util.*
import java.util.function.Function
import java.util.logging.Logger

/**
 * Test descriptor resolver for JUnit Platform Suite test (c.f.
 * https://junit.org/junit5/docs/current/user-guide/#junit-platform-suite-engine)
 */
class JUnitPlatformSuiteDescriptorResolver : ITestDescriptorResolver {
	override fun getUniformPath(testDescriptor: TestDescriptor) =
		extractUniformPathOrClusterId(
			testDescriptor, "uniform path"
		) { it.getUniformPath(testDescriptor) }

	override fun getClusterId(testDescriptor: TestDescriptor) =
		extractUniformPathOrClusterId(
			testDescriptor, "cluster id"
		) { it.getClusterId(testDescriptor) }

	override val engineId: String
		get() = "junit-platform-suite"

	companion object {
		private val LOGGER = getLogger(JUnitPlatformSuiteDescriptorResolver::class.java)

		/** Type of the unique id segment of a test descriptor representing a test suite  */
		private const val SUITE_SEGMENT_TYPE: String = "suite"

		private fun extractUniformPathOrClusterId(
			testDescriptor: TestDescriptor,
			nameOfValueToExtractForLogs: String,
			uniformPathOrClusterIdExtractor: (ITestDescriptorResolver) -> Optional<String>
		): Optional<String> {
			val segments = testDescriptor.uniqueId.segments
			if (verifySegments(segments)) {
				LOGGER.severe {
					"Assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine] for junit-platform-suite tests. Using ${testDescriptor.uniqueId} as $nameOfValueToExtractForLogs as fallback."
				}
				return Optional.of(testDescriptor.uniqueId.toString())
			}

			val suite = segments[1].value.replace('.', '/')
			val secondaryEngineSegments = segments.subList(2, segments.size)

			val secondaryTestDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(
				secondaryEngineSegments[0].value
			)
			if (secondaryTestDescriptorResolver == null) {
				LOGGER.severe {
					"Cannot find a secondary engine nested under the junit-platform-suite engine " +
							"(assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine]). " +
							"Using " + testDescriptor.uniqueId
						.toString() + " as " + nameOfValueToExtractForLogs + " as fallback."
				}
				return Optional.of(testDescriptor.uniqueId.toString())
			}

			val secondaryClusterIdOrUniformPath = uniformPathOrClusterIdExtractor(secondaryTestDescriptorResolver)
			if (!secondaryClusterIdOrUniformPath.isPresent) {
				LOGGER.severe {
					"Secondary test descriptor resolver for engine " +
							secondaryEngineSegments[0]
								.value + " was not able to resolve the " + nameOfValueToExtractForLogs + ". " +
							"Using " + testDescriptor.uniqueId.toString() + " as fallback."
				}
				return Optional.of(testDescriptor.uniqueId.toString())
			}

			return Optional.of(suite + "/" + secondaryClusterIdOrUniformPath.get())
		}

		private fun verifySegments(segments: List<UniqueId.Segment>) =
			segments.size < 3
					|| (segments[0].type != ITestDescriptorResolver.ENGINE_SEGMENT_TYPE)
					|| (segments[1].type != SUITE_SEGMENT_TYPE)
					|| (segments[2].type != ITestDescriptorResolver.ENGINE_SEGMENT_TYPE)
	}
}
