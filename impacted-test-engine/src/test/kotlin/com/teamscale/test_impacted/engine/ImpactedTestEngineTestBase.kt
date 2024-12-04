package com.teamscale.test_impacted.engine

import com.teamscale.client.PrioritizableTestCluster
import com.teamscale.test_impacted.engine.executor.ImpactedTestsProvider
import com.teamscale.test_impacted.engine.executor.ImpactedTestsSorter
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.platform.engine.*
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock

/** Base class for testing specific scenarios in the impacted test engine.  */
abstract class ImpactedTestEngineTestBase {
	private val testEngineRegistry = mock<TestEngineRegistry>()
	private val testDataWriter = mock<TestDataWriter>()
	private val impactedTestsProvider = mock<ImpactedTestsProvider>()
	private val discoveryRequest = mock<EngineDiscoveryRequest>()
	private val executionRequest = mock<ExecutionRequest>()
	private val executionListener = mock<EngineExecutionListener>()
	private val teamscaleAgentNotifier = mock<TeamscaleAgentNotifier>()

	@Test
	fun testEngineExecution() {
		val testEngine = createInternalImpactedTestEngine(engines)

		val engineDescriptor = testEngine
			.discover(discoveryRequest, UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID))
		Assertions.assertThat(engineDescriptor.uniqueId)
			.isEqualTo(UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID))

		Mockito.`when`(executionRequest.engineExecutionListener)
			.thenReturn(executionListener)
		Mockito.`when`(executionRequest.rootTestDescriptor)
			.thenReturn(engineDescriptor)
		Mockito.`when`(impactedTestsProvider.getImpactedTestsFromTeamscale(ArgumentMatchers.any()))
			.thenReturn(impactedTests)

		testEngine.execute(executionRequest)

		verifyCallbacks(executionListener)

		// Ensure test data is written.
		Mockito.verify(testDataWriter).dumpTestDetails(ArgumentMatchers.any())
		Mockito.verify(testDataWriter).dumpTestExecutions(ArgumentMatchers.any())

		Mockito.verifyNoMoreInteractions(executionListener)
		Mockito.verifyNoMoreInteractions(testDataWriter)
	}

	/** Returns the available engines that should be assumed by the impacted test engine.  */
	abstract val engines: List<TestEngine>

	/** Returns the result that Teamscale should return when asked for impacted tests.  */
	abstract val impactedTests: List<PrioritizableTestCluster>

	/** Verifies that the interactions with the executionListener are the ones we would expect.  */
	abstract fun verifyCallbacks(executionListener: EngineExecutionListener)

	private fun createInternalImpactedTestEngine(engines: List<TestEngine>): InternalImpactedTestEngine {
		engines.forEach { engine ->
			Mockito.`when`(testEngineRegistry.getTestEngine(ArgumentMatchers.eq(engine.id)))
				.thenReturn(engine)
		}
		Mockito.`when`<Iterator<TestEngine>>(testEngineRegistry.iterator()).thenReturn(engines.iterator())

		return InternalImpactedTestEngine(
			ImpactedTestEngineConfiguration(
				testDataWriter, testEngineRegistry,
				ImpactedTestsSorter(impactedTestsProvider),
				teamscaleAgentNotifier
			),
			impactedTestsProvider.partition
		)
	}
}
