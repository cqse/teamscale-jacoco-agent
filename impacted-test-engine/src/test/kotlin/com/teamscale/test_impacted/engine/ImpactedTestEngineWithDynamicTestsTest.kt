package com.teamscale.test_impacted.engine

import com.teamscale.client.PrioritizableTest
import com.teamscale.client.PrioritizableTestCluster
import com.teamscale.test_impacted.engine.executor.DummyEngine
import com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver
import org.junit.platform.engine.*
import org.mockito.Mockito

/** Test setup for JUnit Jupiter dynamic tests.  */
internal class ImpactedTestEngineWithDynamicTestsTest : ImpactedTestEngineTestBase() {
	/**
	 * For this test setup we rely on the [JUnitJupiterTestDescriptorResolver] for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private val engineRootId = UniqueId.forEngine("junit-jupiter")

	private val dynamicTestClassId =
		engineRootId.append(JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, "example.DynamicTest")
	private val dynamicTestId =
		dynamicTestClassId.append(JUnitJupiterTestDescriptorResolver.TEST_FACTORY_SEGMENT_TYPE, "testFactory()")
	private val dynamicallyRegisteredTestId =
		dynamicTestId.append(JUnitJupiterTestDescriptorResolver.DYNAMIC_TEST_SEGMENT_TYPE, "#1")

	private val dynamicallyRegisteredTestCase =
		SimpleTestDescriptor.testCase(dynamicallyRegisteredTestId)
	private val dynamicTestCase = SimpleTestDescriptor.dynamicTestCase(
		dynamicTestId,
		dynamicallyRegisteredTestCase
	)
	private val dynamicTestClassCase = SimpleTestDescriptor.testContainer(
		dynamicTestClassId,
		dynamicTestCase
	)
	private val testRoot = SimpleTestDescriptor.testContainer(engineRootId, dynamicTestClassCase)

	override val engines get() = listOf(DummyEngine(testRoot))

	override val impactedTests get() =
		listOf(
			PrioritizableTestCluster(
				"example/DynamicTest",
				listOf(PrioritizableTest("example/DynamicTest/testFactory()"))
			)
		)

	override fun verifyCallbacks(executionListener: EngineExecutionListener) {
		// First the parents test descriptors are started in order.
		Mockito.verify(executionListener).executionStarted(testRoot)
		Mockito.verify(executionListener).executionStarted(dynamicTestClassCase)
		Mockito.verify(executionListener).executionStarted(dynamicTestCase)

		// Test case is added dynamically and executed.
		dynamicTestCase.addChild(dynamicallyRegisteredTestCase)
		Mockito.verify(executionListener).dynamicTestRegistered(dynamicallyRegisteredTestCase)
		Mockito.verify(executionListener).executionStarted(dynamicallyRegisteredTestCase)
		Mockito.verify(executionListener)
			.executionFinished(dynamicallyRegisteredTestCase, TestExecutionResult.successful())

		// Parent test descriptors are also finished.
		Mockito.verify(executionListener).executionFinished(dynamicTestCase, TestExecutionResult.successful())
		Mockito.verify(executionListener).executionFinished(dynamicTestClassCase, TestExecutionResult.successful())
		Mockito.verify(executionListener).executionFinished(testRoot, TestExecutionResult.successful())
	}
}
