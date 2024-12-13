package com.teamscale.test_impacted.engine

import com.teamscale.test_impacted.commons.LoggerUtils.createLogger
import com.teamscale.test_impacted.engine.executor.TestwiseCoverageCollectingExecutionListener
import com.teamscale.test_impacted.test_descriptor.TestDescriptorResolverRegistry.getTestDescriptorResolver
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils.getAvailableTests
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils.getTestDescriptorAsString
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.util.logging.Logger

/**
 * Test engine called internally to allow testing without needing a [ServiceLoader] for [TestEngine] setup.
 */
internal class InternalImpactedTestEngine(
	configuration: ImpactedTestEngineConfiguration,
	private val partition: String
) {
	private val testEngineRegistry = configuration.testEngineRegistry
	private val testSorter = configuration.testSorter
	private val teamscaleAgentNotifier = configuration.teamscaleAgentNotifier
	private val testDataWriter = configuration.testDataWriter

	/**
	 * Performs test discovery by aggregating the result of all [TestEngine]s from the [TestEngineRegistry]
	 * in a single engine [TestDescriptor].
	 */
	fun discover(discoveryRequest: EngineDiscoveryRequest?, uniqueId: UniqueId?): TestDescriptor {
		val engineDescriptor = EngineDescriptor(uniqueId, "Teamscale Impacted Tests")

		LOGGER.fine { "Starting test discovery for engine " + ImpactedTestEngine.ENGINE_ID }

		testEngineRegistry.forEach { delegateTestEngine ->
			LOGGER.fine { "Starting test discovery for delegate engine: " + delegateTestEngine.id }
			val delegateEngineDescriptor = delegateTestEngine.discover(
				discoveryRequest,
				UniqueId.forEngine(delegateTestEngine.id)
			)

			engineDescriptor.addChild(delegateEngineDescriptor)
		}

		LOGGER.fine {
			"Discovered test descriptor for engine ${ImpactedTestEngine.ENGINE_ID}:\n${
				getTestDescriptorAsString(engineDescriptor)
			}"
		}

		return engineDescriptor
	}

	/**
	 * Executes the request by requesting execution of the [TestDescriptor] children aggregated in
	 * [.discover] with the corresponding [org.junit.platform.engine.TestEngine].
	 */
	fun execute(request: ExecutionRequest) {
		val rootTestDescriptor = request.rootTestDescriptor
		val availableTests = getAvailableTests(rootTestDescriptor, partition)

		LOGGER.fine {
			"Starting selection and sorting ${ImpactedTestEngine.ENGINE_ID}:\n${
				getTestDescriptorAsString(rootTestDescriptor)
			}"
		}

		testSorter.selectAndSort(rootTestDescriptor)

		LOGGER.fine {
			"Starting execution of request for engine ${ImpactedTestEngine.ENGINE_ID}:\n${
				getTestDescriptorAsString(rootTestDescriptor)
			}"
		}

		val testExecutions = executeTests(request, rootTestDescriptor)

		testDataWriter.dumpTestExecutions(testExecutions)
		testDataWriter.dumpTestDetails(availableTests.testList)
		teamscaleAgentNotifier.testRunEnded()
	}

	private fun executeTests(request: ExecutionRequest, rootTestDescriptor: TestDescriptor) =
		rootTestDescriptor.children.flatMap { engineTestDescriptor ->
			val engineId = engineTestDescriptor.uniqueId.engineId
			if (!engineId.isPresent) {
				LOGGER.severe { "Engine ID for test descriptor $engineTestDescriptor not present. Skipping execution of the engine." }
				return@flatMap emptyList()
			}

			val testEngine = testEngineRegistry.getTestEngine(engineId.get()) ?: return@flatMap emptyList()
			val testDescriptorResolver = getTestDescriptorResolver(testEngine.id) ?: return@flatMap emptyList()
			val executionListener =
				TestwiseCoverageCollectingExecutionListener(
					teamscaleAgentNotifier,
					testDescriptorResolver,
					request.engineExecutionListener
				)

			testEngine.execute(
				ExecutionRequest(
					engineTestDescriptor, executionListener,
					request.configurationParameters
				)
			)

			executionListener.testExecutions
		}

	companion object {
		private val LOGGER: Logger = createLogger()
	}
}
