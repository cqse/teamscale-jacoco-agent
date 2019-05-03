package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.mockito.Mockito;
import retrofit2.Call;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Tests for {@link InternalImpactedTestEngine}. */
class InternalImpactedTestEngineTest {

	private final TestEngineRegistry testEngineRegistryMock = mock(TestEngineRegistry.class);

	private final TestEngine testEngineMock = mock(TestEngine.class);

	private final TestDataWriter testDataWriter = mock(TestDataWriter.class);

	private final ImpactedTestsProvider impactedTestsProvider = Mockito.mock(ImpactedTestsProvider.class);

	private final EngineDiscoveryRequest discoveryRequest = Mockito.mock(EngineDiscoveryRequest.class);

	private final ExecutionRequest executionRequestMock = mock(ExecutionRequest.class);

	private final EngineExecutionListener executionListenerMock = mock(EngineExecutionListener.class);

	private InternalImpactedTestEngine createInternalImpactedTestEngine(ITestExecutor testExecutor) {
		return new InternalImpactedTestEngine(testEngineRegistryMock, testExecutor, testDataWriter);
	}

	@BeforeEach
	void setupTestEngineMock() {
		when(testEngineRegistryMock.getTestEngine(Mockito.any())).thenReturn(testEngineMock);
		when(testEngineRegistryMock.iterator()).thenReturn(singletonList(testEngineMock).iterator());
		when(testEngineMock.getId()).thenReturn("junit-jupiter");
	}

	@SafeVarargs
	private final void setupTestEngineDiscoveries(Supplier<TestDescriptor>... discoveries) {
		Iterator<Supplier<TestDescriptor>> discoveriesIterator = Arrays.asList(discoveries).iterator();
		when(testEngineMock.discover(any(), any())).thenReturn(discoveriesIterator.next().get());
	}

	@SafeVarargs
	private final void setupTestEngineExecutions(Consumer<EngineExecutionListener>... executions) {
		Iterator<Consumer<EngineExecutionListener>> executionsIterator = Arrays.asList(executions).iterator();

		doAnswer(invocation -> {
			ExecutionRequest request = invocation.getArgument(0);
			EngineExecutionListener executionListener = request.getEngineExecutionListener();
			executionsIterator.next().accept(executionListener);
			return null;
		}).when(testEngineMock).execute(any());
	}

	@Test
	void impactedTestsAreExecutedCorrectly() {
		setupTestEngineMock();

		ITestwiseCoverageAgentApi mockApi = mock(ITestwiseCoverageAgentApi.class);

		when(mockApi.testStarted(Mockito.anyString())).thenReturn(mock(Call.class));
		when(mockApi.testFinished(Mockito.anyString())).thenReturn(mock(Call.class));

		ImpactedTestsExecutor testExecutor = new ImpactedTestsExecutor(
				singletonList(mockApi), impactedTestsProvider);
		InternalImpactedTestEngine internalImpactedTestEngine = createInternalImpactedTestEngine(testExecutor);

		setupTestEngineDiscoveries(
				() -> ImpactedTestsSetup.InitialDiscovery.testEngineRoot,
				()-> ImpactedTestsSetup.FirstImpactedDiscovery.testEngineRoot);
		setupTestEngineExecutions(executionListener -> {
			executionListener.executionStarted(ImpactedTestsSetup.FirstImpactedDiscovery.testEngineRoot);
			executionListener.executionStarted(ImpactedTestsSetup.FirstImpactedDiscovery.testClass);
			executionListener.executionStarted(ImpactedTestsSetup.FirstImpactedDiscovery.testCase1);
			executionListener.executionFinished(ImpactedTestsSetup.FirstImpactedDiscovery.testCase1, successful());
			executionListener.executionFinished(ImpactedTestsSetup.FirstImpactedDiscovery.testClass, successful());
			executionListener.executionFinished(ImpactedTestsSetup.FirstImpactedDiscovery.testEngineRoot, successful());
		});

		TestDescriptor impactedTestEngineDescriptor = internalImpactedTestEngine
				.discover(discoveryRequest, UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));
		assertThat(impactedTestEngineDescriptor.getUniqueId())
				.isEqualTo(UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));

		when(executionRequestMock.getEngineExecutionListener()).thenReturn(executionListenerMock);
		when(executionRequestMock.getRootTestDescriptor()).thenReturn(impactedTestEngineDescriptor);
		when(impactedTestsProvider.getImpactedTestsFromTeamscale(any())).thenReturn(singletonList(
				new PrioritizableTestCluster("TestClassA",
						singletonList(new PrioritizableTest("TestClassA/testCase1()")))));

		internalImpactedTestEngine.execute(executionRequestMock);

		verify(executionListenerMock).executionStarted(impactedTestEngineDescriptor);
		verify(executionListenerMock).executionStarted(ImpactedTestsSetup.InitialDiscovery.testEngineRoot);
		verify(executionListenerMock).executionStarted(ImpactedTestsSetup.InitialDiscovery.testClass);
		verify(executionListenerMock).executionStarted(ImpactedTestsSetup.InitialDiscovery.testCase1);
		verify(executionListenerMock).executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.testCase1), any());
		verify(executionListenerMock).executionSkipped(eq(ImpactedTestsSetup.InitialDiscovery.testCase2), any());
		verify(executionListenerMock).executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.testClass), any());
		verify(executionListenerMock).executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.testEngineRoot), any());
		verify(executionListenerMock).executionFinished(eq(impactedTestEngineDescriptor), any());
		verifyNoMoreInteractions(executionListenerMock);
	}

	private static class ImpactedTestsSetup {

		private static final UniqueId engineRootId = UniqueId.forEngine("junit-jupiter");
		private static final UniqueId testClassId = engineRootId.append("class", "TestClassA");
		private static final UniqueId testCase1Id = testClassId.append("method", "testCase1()");
		private static final UniqueId testCase2Id = testClassId.append("method", "testCase2()");

		private static class InitialDiscovery {
			private static final TestDescriptor testCase1 = testCase(testCase1Id);
			private static final TestDescriptor testCase2 = testCase(testCase2Id);
			private static final TestDescriptor testClass = testContainer(testClassId, testCase1, testCase2);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, testClass);
		}

		private static class FirstImpactedDiscovery {
			private static final TestDescriptor testCase1 = testCase(testCase1Id);
			private static final TestDescriptor testClass = testContainer(testClassId, testCase1);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, testClass);
		}
	}
}