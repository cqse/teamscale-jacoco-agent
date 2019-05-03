package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.mockito.Mockito;
import retrofit2.Call;

import java.util.List;
import java.util.Optional;

import static com.teamscale.test_impacted.engine.executor.AutoSkippingEngineExecutionListener.TEST_NOT_IMPACTED_REASON;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Tests for {@link TestwiseCoverageCollectingExecutionListener}. */
class TestwiseCoverageCollectingExecutionListenerTest {

	private final ITestwiseCoverageAgentApi mockApi = mock(ITestwiseCoverageAgentApi.class);

	private final ITestDescriptorResolver resolver = mock(ITestDescriptorResolver.class);

	private final EngineExecutionListener executionListenerMock = mock(EngineExecutionListener.class);

	private final TestwiseCoverageCollectingExecutionListener executionListener = new TestwiseCoverageCollectingExecutionListener(
			singletonList(mockApi), resolver, executionListenerMock);

	private final UniqueId rootId = UniqueId.forEngine("dummy");

	@Test
	void testInteractionWithListenersAndCoverageApi() {
		UniqueId testClassId = rootId.append("TEST_CONTAINER", "MyClass");
		UniqueId impactedTestCaseId = testClassId.append("TEST_CASE", "impactedTestCase()");
		UniqueId nonImpactedTestCaseId = testClassId.append("TEST_CASE", "nonImpactedTestCase()");
		UniqueId regularSkippedTestCaseId = testClassId.append("TEST_CASE", "regularSkippedTestCase()");

		TestDescriptor impactedTestCase = testCase(impactedTestCaseId);
		TestDescriptor nonImpactedTestCase = testCase(nonImpactedTestCaseId);
		TestDescriptor regularSkippedTestCase = testCase(regularSkippedTestCaseId);
		TestDescriptor testClass = testContainer(testClassId, impactedTestCase, nonImpactedTestCase,
				regularSkippedTestCase);
		TestDescriptor testRoot = testContainer(rootId, testClass);

		when(resolver.getUniformPath(impactedTestCase)).thenReturn(Optional.of("MyClass/impactedTestCase()"));
		when(resolver.getClusterId(impactedTestCase)).thenReturn(Optional.of("MyClass"));
		when(resolver.getUniformPath(nonImpactedTestCase))
				.thenReturn(Optional.of("MyClass/nonImpactedTestCase()"));
		when(resolver.getClusterId(nonImpactedTestCase)).thenReturn(Optional.of("MyClass"));
		when(resolver.getUniformPath(regularSkippedTestCase))
				.thenReturn(Optional.of("MyClass/regularSkippedTestCase()"));
		when(resolver.getClusterId(regularSkippedTestCase)).thenReturn(Optional.of("MyClass"));
		when(mockApi.testStarted(Mockito.anyString())).thenReturn(mock(Call.class));
		when(mockApi.testFinished(Mockito.anyString())).thenReturn(mock(Call.class));

		// Start engine and class.
		executionListener.executionStarted(testRoot);
		verify(executionListenerMock).executionStarted(testRoot);
		executionListener.executionStarted(testClass);
		verify(executionListenerMock).executionStarted(testClass);

		// Execution of impacted test case.
		executionListener.executionStarted(impactedTestCase);
		verify(mockApi).testStarted("MyClass/impactedTestCase()");
		verify(executionListenerMock).executionStarted(impactedTestCase);
		executionListener.executionFinished(impactedTestCase, successful());
		verify(mockApi).testFinished("MyClass/impactedTestCase()");
		verify(executionListenerMock).executionFinished(impactedTestCase, successful());

		// Non impacted test case is skipped.
		executionListener.executionSkipped(nonImpactedTestCase, TEST_NOT_IMPACTED_REASON);
		verify(executionListenerMock).executionSkipped(nonImpactedTestCase, TEST_NOT_IMPACTED_REASON);

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
		assertThat(testExecutions).allSatisfy(testExecution -> {
			assertThat(testExecution.getUniformPath()).isNotEqualTo("MyClass/nonImpactedTestCase()");
		});
		assertThat(testExecutions).anySatisfy(testExecution -> {
			assertThat(testExecution.getUniformPath()).isEqualTo("MyClass/impactedTestCase()");
		});
		assertThat(testExecutions).anySatisfy(testExecution -> {
			assertThat(testExecution.getUniformPath()).isEqualTo("MyClass/regularSkippedTestCase()");
		});
	}
}