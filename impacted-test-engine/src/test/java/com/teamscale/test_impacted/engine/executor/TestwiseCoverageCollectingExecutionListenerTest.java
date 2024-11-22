package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.List;
import java.util.Optional;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Tests for {@link TestwiseCoverageCollectingExecutionListener}. */
class TestwiseCoverageCollectingExecutionListenerTest {

	private final TeamscaleAgentNotifier mockApi = mock(TeamscaleAgentNotifier.class);

	private final ITestDescriptorResolver resolver = mock(ITestDescriptorResolver.class);

	private final EngineExecutionListener executionListenerMock = mock(EngineExecutionListener.class);

	private final TestwiseCoverageCollectingExecutionListener executionListener = new TestwiseCoverageCollectingExecutionListener(
			mockApi, resolver, executionListenerMock);

	private final UniqueId rootId = UniqueId.forEngine("dummy");

	@Test
	void testInteractionWithListenersAndCoverageApi() {
		UniqueId testClassId = rootId.append("TEST_CONTAINER", "MyClass");
		UniqueId impactedTestCaseId = testClassId.append("TEST_CASE", "impactedTestCase()");
		UniqueId regularSkippedTestCaseId = testClassId.append("TEST_CASE", "regularSkippedTestCase()");

		TestDescriptor impactedTestCase = testCase(impactedTestCaseId);
		TestDescriptor regularSkippedTestCase = testCase(regularSkippedTestCaseId);
		TestDescriptor testClass = testContainer(testClassId, impactedTestCase,
				regularSkippedTestCase);
		TestDescriptor testRoot = testContainer(rootId, testClass);

		when(resolver.getUniformPath(impactedTestCase)).thenReturn(Optional.of("MyClass/impactedTestCase()"));
		when(resolver.getClusterId(impactedTestCase)).thenReturn(Optional.of("MyClass"));
		when(resolver.getUniformPath(regularSkippedTestCase))
				.thenReturn(Optional.of("MyClass/regularSkippedTestCase()"));
		when(resolver.getClusterId(regularSkippedTestCase)).thenReturn(Optional.of("MyClass"));

		// Start engine and class.
		executionListener.executionStarted(testRoot);
		verify(executionListenerMock).executionStarted(testRoot);
		executionListener.executionStarted(testClass);
		verify(executionListenerMock).executionStarted(testClass);

		// Execution of impacted test case.
		executionListener.executionStarted(impactedTestCase);
		verify(mockApi).startTest("MyClass/impactedTestCase()");
		verify(executionListenerMock).executionStarted(impactedTestCase);
		executionListener.executionFinished(impactedTestCase, successful());
		verify(mockApi).endTest(eq("MyClass/impactedTestCase()"), any());
		verify(executionListenerMock).executionFinished(impactedTestCase, successful());

		// Ignored or disabled impacted test case is skipped.
		executionListener.executionSkipped(regularSkippedTestCase, "Test is disabled.");
		verify(executionListenerMock).executionSkipped(regularSkippedTestCase, "Test is disabled.");

		// Finish class and engine.
		executionListener.executionFinished(testClass, successful());
		verify(executionListenerMock).executionFinished(testClass, successful());
		executionListener.executionFinished(testRoot, successful());
		verify(executionListenerMock).executionFinished(testRoot, successful());

		verifyNoMoreInteractions(mockApi);
		verifyNoMoreInteractions(executionListenerMock);

		List<TestExecution> testExecutions = executionListener.getTestExecutions();

		assertThat(testExecutions).hasSize(2);
		assertThat(testExecutions).anySatisfy(testExecution ->
				assertThat(testExecution.uniformPath).isEqualTo("MyClass/impactedTestCase()"));
		assertThat(testExecutions).anySatisfy(testExecution ->
				assertThat(testExecution.uniformPath).isEqualTo("MyClass/regularSkippedTestCase()"));
	}

	@Test
	void testSkipOfTestClass() {
		UniqueId testClassId = rootId.append("TEST_CONTAINER", "MyClass");
		UniqueId testCase1Id = testClassId.append("TEST_CASE", "testCase1()");
		UniqueId testCase2Id = testClassId.append("TEST_CASE", "testCase2()");

		TestDescriptor testCase1 = testCase(testCase1Id);
		TestDescriptor testCase2 = testCase(testCase2Id);
		TestDescriptor testClass = testContainer(testClassId, testCase1, testCase2);
		TestDescriptor testRoot = testContainer(rootId, testClass);

		when(resolver.getUniformPath(testCase1)).thenReturn(Optional.of("MyClass/testCase1()"));
		when(resolver.getClusterId(testCase1)).thenReturn(Optional.of("MyClass"));
		when(resolver.getUniformPath(testCase2)).thenReturn(Optional.of("MyClass/testCase2()"));
		when(resolver.getClusterId(testCase2)).thenReturn(Optional.of("MyClass"));

		// Start engine and class.
		executionListener.executionStarted(testRoot);
		verify(executionListenerMock).executionStarted(testRoot);

		executionListener.executionSkipped(testClass, "Test class is disabled.");
		verify(executionListenerMock).executionStarted(testClass);
		verify(executionListenerMock).executionSkipped(testCase1, "Test class is disabled.");
		verify(executionListenerMock).executionSkipped(testCase2, "Test class is disabled.");
		verify(executionListenerMock).executionFinished(testClass, successful());

		executionListener.executionFinished(testRoot, successful());
		verify(executionListenerMock).executionFinished(testRoot, successful());

		verifyNoMoreInteractions(executionListenerMock);

		List<TestExecution> testExecutions = executionListener.getTestExecutions();

		assertThat(testExecutions).hasSize(2);
		assertThat(testExecutions)
				.allMatch(testExecution -> testExecution.result.equals(ETestExecutionResult.SKIPPED));
	}
}