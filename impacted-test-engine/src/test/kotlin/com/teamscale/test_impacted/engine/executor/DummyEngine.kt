package com.teamscale.test_impacted.engine.executor

import org.junit.platform.engine.*

/**
 * A test engine that simulates the behavior of the vintage and jupiter engine that the impacted test engine invokes
 * under the hood.
 */
class DummyEngine(private val descriptor: TestDescriptor) : TestEngine {
	override fun getId() = descriptor.uniqueId.engineId.get()

	override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId) =
		descriptor

	override fun execute(request: ExecutionRequest) {
		val executionListener = request.engineExecutionListener
		this.executeDescriptor(executionListener, request.rootTestDescriptor)
	}

	/**
	 * Calls the [EngineExecutionListener] callbacks in the expected order. The information whether tests should
	 * be skipped, should fail or have dynamic executions is attached to the [SimpleTestDescriptor].
	 */
	private fun executeDescriptor(executionListener: EngineExecutionListener, testDescriptor: TestDescriptor?) {
		require(testDescriptor is SimpleTestDescriptor) {
			"Expected TestDescriptor to be of type SimpleTestDescriptor"
		}
		if (testDescriptor.shouldBeSkipped()) {
			executionListener.executionSkipped(testDescriptor, "Tests class is disabled.")
			return
		}
		executionListener.executionStarted(testDescriptor)
		testDescriptor.getChildren().forEach { child ->
			executeDescriptor(executionListener, child)
		}
		testDescriptor.dynamicTests.forEach { dynamicTest ->
			testDescriptor.addChild(dynamicTest)
			executionListener.dynamicTestRegistered(dynamicTest)
			executeDescriptor(executionListener, dynamicTest)
		}
		executionListener.executionFinished(
			testDescriptor, testDescriptor.executionResult
		)
	}
}
