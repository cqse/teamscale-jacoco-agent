package com.teamscale.test_impacted.engine

import com.teamscale.client.PrioritizableTestCluster
import com.teamscale.test_impacted.engine.executor.DummyEngine
import com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.UniqueId
import org.mockito.kotlin.*

/** Test setup where no test is impacted.  */
internal class NoImpactedTestsTest : ImpactedTestEngineTestBase() {
	/**
	 * For this test setup we rely on the [JUnitJupiterTestDescriptorResolver] for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private val engine1RootId = UniqueId.forEngine("junit-jupiter")

	/** FirstTestClass contains one non-impacted test.  */
	private val firstTestClassId = engine1RootId.append(
		JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, FIRST_TEST_CLASS
	)
	private val nonImpactedTestCase1Id = firstTestClassId
		.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, NON_IMPACTED_TEST_CASE_1)
	private val nonImpactedTestCase1 = SimpleTestDescriptor.testCase(nonImpactedTestCase1Id)
	private val firstTestClass =
		SimpleTestDescriptor.testContainer(firstTestClassId, nonImpactedTestCase1)

	private val testEngine1Root = SimpleTestDescriptor.testContainer(engine1RootId, firstTestClass)

	override val engines = listOf(DummyEngine(testEngine1Root))
	override val impactedTests = emptyList<PrioritizableTestCluster>()

	override fun verifyCallbacks(executionListener: EngineExecutionListener) {
		// Verify that the root container (engine) starts and finishes
		verify(executionListener).executionStarted(testEngine1Root)
		verify(executionListener).executionFinished(eq(testEngine1Root), any())

		// Verify that each non-impacted test starts and finishes correctly
		verify(executionListener).executionStarted(firstTestClass)
		verify(executionListener).executionFinished(eq(firstTestClass), any())

		verify(executionListener).executionStarted(nonImpactedTestCase1)
		verify(executionListener).executionFinished(eq(nonImpactedTestCase1), any())
	}

	companion object {
		private const val FIRST_TEST_CLASS = "FirstTestClass"
		private const val NON_IMPACTED_TEST_CASE_1 = "nonImpactedTestCase1()"
	}
}
