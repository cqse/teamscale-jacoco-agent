package com.teamscale.test_impacted.engine

import com.teamscale.client.PrioritizableTest
import com.teamscale.client.PrioritizableTestCluster
import com.teamscale.test_impacted.engine.executor.DummyEngine
import com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver
import org.junit.platform.engine.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.util.*

/** Test setup for a mixture of impacted and no impacted tests and two test engines.  */
internal class ImpactedTestEngineWithTwoEnginesTest : ImpactedTestEngineTestBase() {
	/**
	 * For this test setup we rely on the [JUnitJupiterTestDescriptorResolver] for resolving uniform paths and
	 * cluster ids. Therefore, the engine root is set accordingly.
	 */
	private val engine1RootId = UniqueId.forEngine("junit-jupiter")
	private val engine2RootId = UniqueId.forEngine("exotic-engine")

	/** FirstTestClass contains one impacted and one non-impacted test.  */
	private val firstTestClassId = engine1RootId.append(
		JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, FIRST_TEST_CLASS
	)
	private val impactedTestCase1Id = firstTestClassId.append(
		JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
		IMPACTED_TEST_CASE_1
	)
	private val nonImpactedTestCase1Id = firstTestClassId
		.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, NON_IMPACTED_TEST_CASE_1)

	/**
	 * IgnoredTestClass is ignored (e.g. class is annotated with [Disabled]). Hence, it'll be impacted since it
	 * was previously skipped.
	 */
	private val ignoredTestClassId = engine1RootId.append(
		JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, IGNORED_TEST_CLASS
	)
	private val impactedTestCase2Id = ignoredTestClassId.append(
		JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
		IMPACTED_TEST_CASE_2
	)
	private val nonImpactedTestCase2Id = ignoredTestClassId
		.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, NON_IMPACTED_TEST_CASE_2)

	/**
	 * ImpactedTestClassWithSkippedTest contains two impacted tests of which one is skipped.
	 */
	private val secondTestClassId = engine1RootId.append(
		JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, SECOND_TEST_CLASS
	)
	private val impactedTestCase3Id = secondTestClassId.append(
		JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
		IMPACTED_TEST_CASE_3
	)
	private val skippedImpactedTestCaseId = secondTestClassId
		.append(JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE, SKIPPED_IMPACTED_TEST_CASE_ID)

	/** OtherTestClass contains one impacted and one non-impacted test.  */
	private val otherTestClassId = engine2RootId.append(
		JUnitJupiterTestDescriptorResolver.CLASS_SEGMENT_TYPE, OTHER_TEST_CLASS
	)
	private val impactedTestCase4Id = otherTestClassId.append(
		JUnitJupiterTestDescriptorResolver.METHOD_SEGMENT_TYPE,
		IMPACTED_TEST_CASE_4
	)

	private val impactedTestCase1 = SimpleTestDescriptor.testCase(impactedTestCase1Id)
	private val nonImpactedTestCase1 = SimpleTestDescriptor.testCase(nonImpactedTestCase1Id)
	private val firstTestClass = SimpleTestDescriptor.testContainer(firstTestClassId, impactedTestCase1, nonImpactedTestCase1)

	private val impactedTestCase2 = SimpleTestDescriptor.testCase(impactedTestCase2Id)
	private val nonImpactedTestCase2 = SimpleTestDescriptor.testCase(nonImpactedTestCase2Id)
	private val ignoredTestClass = SimpleTestDescriptor.testContainer(
		ignoredTestClassId, impactedTestCase2, nonImpactedTestCase2
	).skip()

	private val failed = TestExecutionResult.failed(NullPointerException())
	private val impactedTestCase3 = SimpleTestDescriptor.testCase(impactedTestCase3Id).result(failed)
	private val skippedImpactedTestCase =
		SimpleTestDescriptor.testCase(skippedImpactedTestCaseId).skip()
	private val secondTestClass = SimpleTestDescriptor.testContainer(
		secondTestClassId,
		impactedTestCase3,
		skippedImpactedTestCase
	)

	private val impactedTestCase4 = SimpleTestDescriptor.testCase(impactedTestCase4Id)
	private val otherTestClass = SimpleTestDescriptor.testContainer(otherTestClassId, impactedTestCase4)

	private val testEngine1Root = SimpleTestDescriptor.testContainer(
		engine1RootId, firstTestClass,
		ignoredTestClass, secondTestClass
	)

	private val testEngine2Root = SimpleTestDescriptor.testContainer(engine2RootId, otherTestClass)

	override val engines = listOf(
		DummyEngine(testEngine1Root),
		DummyEngine(testEngine2Root)
	)

	override val impactedTests =
		listOf(
			PrioritizableTestCluster(
				FIRST_TEST_CLASS,
				listOf(PrioritizableTest("$FIRST_TEST_CLASS/$IMPACTED_TEST_CASE_1"))
			),
			PrioritizableTestCluster(
				OTHER_TEST_CLASS,
				listOf(PrioritizableTest("$OTHER_TEST_CLASS/$IMPACTED_TEST_CASE_4"))
			),
			PrioritizableTestCluster(
				IGNORED_TEST_CLASS,
				listOf(PrioritizableTest("$IGNORED_TEST_CLASS/$IMPACTED_TEST_CASE_2"))
			),
			PrioritizableTestCluster(
				SECOND_TEST_CLASS,
				listOf(
					PrioritizableTest("$SECOND_TEST_CLASS/$IMPACTED_TEST_CASE_3"),
					PrioritizableTest("$SECOND_TEST_CLASS/$SKIPPED_IMPACTED_TEST_CASE_ID")
				)
			)
		)

	override fun verifyCallbacks(executionListener: EngineExecutionListener) {
		// Start of engine 1
		verify(executionListener).executionStarted(testEngine1Root)

		// Execute FirstTestClass.
		verify(executionListener).executionStarted(firstTestClass)
		verify(executionListener).executionStarted(impactedTestCase1)
		verify(executionListener)
			.executionFinished(eq(impactedTestCase1), any())
		verify(executionListener).executionFinished(eq(firstTestClass), any())

		// Execute IgnoredTestClass.
		verify(executionListener).executionStarted(ignoredTestClass)
		verify(executionListener)
			.executionSkipped(eq(impactedTestCase2), any())
		verify(executionListener).executionFinished(ignoredTestClass, TestExecutionResult.successful())

		// Execute SecondTestClass.
		verify(executionListener).executionStarted(secondTestClass)
		verify(executionListener).executionStarted(eq(impactedTestCase3))
		verify(executionListener).executionFinished(impactedTestCase3, failed)
		verify(executionListener)
			.executionSkipped(eq(skippedImpactedTestCase), any())
		verify(executionListener).executionFinished(secondTestClass, TestExecutionResult.successful())

		// Finish test engine 1
		verify(executionListener).executionFinished(testEngine1Root, TestExecutionResult.successful())

		// Start of engine 2
		verify(executionListener).executionStarted(testEngine2Root)

		// Execute OtherTestClass.
		verify(executionListener).executionStarted(otherTestClass)
		verify(executionListener).executionStarted(impactedTestCase4)
		verify(executionListener).executionFinished(impactedTestCase4, TestExecutionResult.successful())
		verify(executionListener).executionFinished(otherTestClass, TestExecutionResult.successful())

		// Finish test engine 2
		verify(executionListener).executionFinished(testEngine2Root, TestExecutionResult.successful())
	}

	companion object {
		private const val FIRST_TEST_CLASS = "FirstTestClass"
		private const val OTHER_TEST_CLASS = "OtherTestClass"
		private const val IGNORED_TEST_CLASS = "IgnoredTestClass"
		private const val SECOND_TEST_CLASS = "SecondTestClass"
		private const val IMPACTED_TEST_CASE_1 = "impactedTestCase1()"
		private const val IMPACTED_TEST_CASE_2 = "impactedTestCase2()"
		private const val IMPACTED_TEST_CASE_3 = "impactedTestCase3()"
		private const val IMPACTED_TEST_CASE_4 = "impactedTestCase4()"
		private const val SKIPPED_IMPACTED_TEST_CASE_ID = "skippedImpactedTestCaseId()"
		private const val NON_IMPACTED_TEST_CASE_1 = "nonImpactedTestCase1()"
		private const val NON_IMPACTED_TEST_CASE_2 = "nonImpactedTestCase2()"
	}
}
