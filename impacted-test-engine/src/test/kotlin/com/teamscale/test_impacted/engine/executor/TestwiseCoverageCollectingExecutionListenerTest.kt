package com.teamscale.test_impacted.engine.executor

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.util.*

/** Tests for [TestwiseCoverageCollectingExecutionListener].  */
internal class TestwiseCoverageCollectingExecutionListenerTest {
	private val mockApi = mock<TeamscaleAgentNotifier>()

	private val resolver = mock<ITestDescriptorResolver>()

	private val executionListenerMock = mock<EngineExecutionListener>()

	private val executionListener = TestwiseCoverageCollectingExecutionListener(
		mockApi, resolver, executionListenerMock
	)

	private val rootId = UniqueId.forEngine("dummy")

	@Test
	fun testInteractionWithListenersAndCoverageApi() {
		val testClassId = rootId.append("TEST_CONTAINER", "MyClass")
		val impactedTestCaseId = testClassId.append("TEST_CASE", "impactedTestCase()")
		val regularSkippedTestCaseId = testClassId.append("TEST_CASE", "regularSkippedTestCase()")

		val impactedTestCase = SimpleTestDescriptor.testCase(impactedTestCaseId)
		val regularSkippedTestCase = SimpleTestDescriptor.testCase(regularSkippedTestCaseId)
		val testClass = SimpleTestDescriptor.testContainer(
			testClassId, impactedTestCase,
			regularSkippedTestCase
		)
		val testRoot = SimpleTestDescriptor.testContainer(rootId, testClass)

		Mockito.`when`(resolver.getUniformPath(impactedTestCase))
			.thenReturn(Optional.of("MyClass/impactedTestCase()"))
		Mockito.`when`(resolver.getClusterId(impactedTestCase))
			.thenReturn(Optional.of("MyClass"))
		Mockito.`when`(resolver.getUniformPath(regularSkippedTestCase))
			.thenReturn(Optional.of("MyClass/regularSkippedTestCase()"))
		Mockito.`when`(resolver.getClusterId(regularSkippedTestCase))
			.thenReturn(Optional.of("MyClass"))

		// Start engine and class.
		executionListener.executionStarted(testRoot)
		Mockito.verify(executionListenerMock).executionStarted(testRoot)
		executionListener.executionStarted(testClass)
		Mockito.verify(executionListenerMock).executionStarted(testClass)

		// Execution of impacted test case.
		executionListener.executionStarted(impactedTestCase)
		Mockito.verify(mockApi).startTest("MyClass/impactedTestCase()")
		Mockito.verify(executionListenerMock).executionStarted(impactedTestCase)
		executionListener.executionFinished(impactedTestCase, TestExecutionResult.successful())
		Mockito.verify(mockApi).endTest(ArgumentMatchers.eq("MyClass/impactedTestCase()"), ArgumentMatchers.any())
		Mockito.verify(executionListenerMock).executionFinished(impactedTestCase, TestExecutionResult.successful())

		// Ignored or disabled impacted test case is skipped.
		executionListener.executionSkipped(regularSkippedTestCase, "Test is disabled.")
		Mockito.verify(executionListenerMock).executionSkipped(regularSkippedTestCase, "Test is disabled.")

		// Finish class and engine.
		executionListener.executionFinished(testClass, TestExecutionResult.successful())
		Mockito.verify(executionListenerMock).executionFinished(testClass, TestExecutionResult.successful())
		executionListener.executionFinished(testRoot, TestExecutionResult.successful())
		Mockito.verify(executionListenerMock).executionFinished(testRoot, TestExecutionResult.successful())

		Mockito.verifyNoMoreInteractions(mockApi)
		Mockito.verifyNoMoreInteractions(executionListenerMock)

		val testExecutions = executionListener.testExecutions

		Assertions.assertThat(testExecutions).hasSize(2)
		Assertions.assertThat(testExecutions).anySatisfy { testExecution: TestExecution ->
			Assertions.assertThat(
				testExecution.uniformPath
			).isEqualTo("MyClass/impactedTestCase()")
		}
		Assertions.assertThat(testExecutions).anySatisfy { testExecution: TestExecution ->
			Assertions.assertThat(
				testExecution.uniformPath
			).isEqualTo("MyClass/regularSkippedTestCase()")
		}
	}

	@Test
	fun testSkipOfTestClass() {
		val testClassId = rootId.append("TEST_CONTAINER", "MyClass")
		val testCase1Id = testClassId.append("TEST_CASE", "testCase1()")
		val testCase2Id = testClassId.append("TEST_CASE", "testCase2()")

		val testCase1: TestDescriptor = SimpleTestDescriptor.testCase(testCase1Id)
		val testCase2: TestDescriptor = SimpleTestDescriptor.testCase(testCase2Id)
		val testClass: TestDescriptor = SimpleTestDescriptor.testContainer(testClassId, testCase1, testCase2)
		val testRoot: TestDescriptor = SimpleTestDescriptor.testContainer(rootId, testClass)

		Mockito.`when`(resolver.getUniformPath(testCase1))
			.thenReturn(Optional.of("MyClass/testCase1()"))
		Mockito.`when`(resolver.getClusterId(testCase1))
			.thenReturn(Optional.of("MyClass"))
		Mockito.`when`(resolver.getUniformPath(testCase2))
			.thenReturn(Optional.of("MyClass/testCase2()"))
		Mockito.`when`(resolver.getClusterId(testCase2))
			.thenReturn(Optional.of("MyClass"))

		// Start engine and class.
		executionListener.executionStarted(testRoot)
		Mockito.verify(executionListenerMock).executionStarted(testRoot)

		executionListener.executionSkipped(testClass, "Test class is disabled.")
		Mockito.verify(executionListenerMock).executionStarted(testClass)
		Mockito.verify(executionListenerMock).executionSkipped(testCase1, "Test class is disabled.")
		Mockito.verify(executionListenerMock).executionSkipped(testCase2, "Test class is disabled.")
		Mockito.verify(executionListenerMock).executionFinished(testClass, TestExecutionResult.successful())

		executionListener.executionFinished(testRoot, TestExecutionResult.successful())
		Mockito.verify(executionListenerMock).executionFinished(testRoot, TestExecutionResult.successful())

		Mockito.verifyNoMoreInteractions(executionListenerMock)

		val testExecutions = executionListener.testExecutions

		Assertions.assertThat(testExecutions).hasSize(2)
		Assertions.assertThat(testExecutions)
			.allMatch { it.result == ETestExecutionResult.SKIPPED }
	}
}