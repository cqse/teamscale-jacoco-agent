package com.teamscale.test_impacted.engine

import com.teamscale.client.PrioritizableTest
import com.teamscale.client.PrioritizableTestCluster
import com.teamscale.test_impacted.engine.executor.DummyEngine
import com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.UniqueId
import org.mockito.kotlin.verifyNoInteractions

/** Test setup where the engine was not consciously enabled.  */
internal class DisabledImpactedTestsEngineTest : ImpactedTestEngineTestBase() {
	/**
	 * For this test setup we rely on the [JUnitJupiterTestDescriptorResolver] for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private val engine1RootId = UniqueId.forEngine("junit-jupiter")

	/** FirstTestClass contains one non-impacted test.  */
	private val firstTestClassId = engine1RootId.append(
		JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, FIRST_TEST_CLASS
	)
	private val testCase1Id = firstTestClassId
		.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, TEST_CASE_1)
	private val testCase1 = SimpleTestDescriptor.testCase(testCase1Id)
	private val firstTestClass =
		SimpleTestDescriptor.testContainer(firstTestClassId, testCase1)

	private val testEngine1Root = SimpleTestDescriptor.testContainer(engine1RootId, firstTestClass)

	override val enabled: Boolean = false

	override val engines = listOf(DummyEngine(testEngine1Root))
	override val impactedTests = listOf(
		PrioritizableTestCluster(
			FIRST_TEST_CLASS,
			listOf(PrioritizableTest("${FIRST_TEST_CLASS}/$TEST_CASE_1"))
		)
	)

	override fun verifyCallbacks(executionListener: EngineExecutionListener) {
		// Verify that no tests have been executed
		verifyNoInteractions(executionListener)
	}

	companion object {
		private const val FIRST_TEST_CLASS = "FirstTestClass"
		private const val TEST_CASE_1 = "testCase1()"
	}
}
