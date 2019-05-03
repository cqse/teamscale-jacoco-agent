package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsProvider;
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.mockito.Mockito;
import retrofit2.Call;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Arrays.asList;
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

	private final TestEngineRegistry testEngineRegistry = mock(TestEngineRegistry.class);

	private final TestEngine testEngine = mock(TestEngine.class);

	private final TestDataWriter testDataWriter = mock(TestDataWriter.class);

	private final ImpactedTestsProvider impactedTestsProvider = Mockito.mock(ImpactedTestsProvider.class);

	private final EngineDiscoveryRequest discoveryRequest = Mockito.mock(EngineDiscoveryRequest.class);

	private final ExecutionRequest executionRequest = mock(ExecutionRequest.class);

	private final EngineExecutionListener executionListener = mock(EngineExecutionListener.class);

	private final ITestwiseCoverageAgentApi testwiseCoverageAgentApi = mock(ITestwiseCoverageAgentApi.class);

	private InternalImpactedTestEngine createInternalImpactedTestEngine(ITestExecutor testExecutor) {
		return new InternalImpactedTestEngine(testEngineRegistry, testExecutor, testDataWriter);
	}

	@BeforeEach
	void setupTestEngineAndCoverageAgent() {
		when(testEngineRegistry.getTestEngine(Mockito.any())).thenReturn(testEngine);
		when(testEngineRegistry.iterator()).thenReturn(singletonList(testEngine).iterator());
		when(testEngine.getId()).thenReturn("junit-jupiter");
		when(testwiseCoverageAgentApi.testStarted(Mockito.anyString())).thenReturn(mock(Call.class));
		when(testwiseCoverageAgentApi.testFinished(Mockito.anyString())).thenReturn(mock(Call.class));
	}

	@SafeVarargs
	private final void setupTestEngineDiscoveries(Supplier<TestDescriptor>... discoveries) {
		Iterator<Supplier<TestDescriptor>> discoveriesIterator = asList(discoveries).iterator();
		when(testEngine.discover(any(), any())).thenReturn(discoveriesIterator.next().get());
	}

	@SafeVarargs
	private final void setupTestEngineExecutions(Consumer<EngineExecutionListener>... executions) {
		Iterator<Consumer<EngineExecutionListener>> executionsIterator = asList(executions).iterator();

		doAnswer(invocation -> {
			ExecutionRequest request = invocation.getArgument(0);
			EngineExecutionListener executionListener = request.getEngineExecutionListener();
			executionsIterator.next().accept(executionListener);
			return null;
		}).when(testEngine).execute(any());
	}

	@Test
	void impactedTestsAreExecutedCorrectly() {
		ImpactedTestsExecutor testExecutor = new ImpactedTestsExecutor(
				singletonList(testwiseCoverageAgentApi), impactedTestsProvider);
		InternalImpactedTestEngine internalImpactedTestEngine = createInternalImpactedTestEngine(testExecutor);

		setupTestEngineDiscoveries(
				() -> ImpactedTestsSetup.InitialDiscovery.testEngineRoot,
				() -> ImpactedTestsSetup.TestClassADiscovery.testEngineRoot,
				() -> ImpactedTestsSetup.IgnoredTestClassDiscovery.testEngineRoot);
		setupTestEngineExecutions(executionListener -> {
			executionListener.executionStarted(ImpactedTestsSetup.TestClassADiscovery.testEngineRoot);
			executionListener.executionStarted(ImpactedTestsSetup.TestClassADiscovery.testClassA);
			executionListener.executionStarted(ImpactedTestsSetup.TestClassADiscovery.impactedTestCaseA);
			executionListener.executionFinished(ImpactedTestsSetup.TestClassADiscovery.impactedTestCaseA, successful());
			executionListener.executionFinished(ImpactedTestsSetup.TestClassADiscovery.testClassA, successful());
			executionListener.executionFinished(ImpactedTestsSetup.TestClassADiscovery.testEngineRoot, successful());
		}, executionListener -> {
			executionListener.executionStarted(ImpactedTestsSetup.IgnoredTestClassDiscovery.testEngineRoot);
			executionListener.executionSkipped(ImpactedTestsSetup.IgnoredTestClassDiscovery.ignoredTestClass,
					"Tests class is disabled.");
			executionListener
					.executionFinished(ImpactedTestsSetup.IgnoredTestClassDiscovery.testEngineRoot, successful());
		});

		TestDescriptor impactedTestEngineDescriptor = internalImpactedTestEngine
				.discover(discoveryRequest, UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));
		assertThat(impactedTestEngineDescriptor.getUniqueId())
				.isEqualTo(UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));

		when(executionRequest.getEngineExecutionListener()).thenReturn(executionListener);
		when(executionRequest.getRootTestDescriptor()).thenReturn(impactedTestEngineDescriptor);
		when(impactedTestsProvider.getImpactedTestsFromTeamscale(any())).thenReturn(asList(
				new PrioritizableTestCluster("TestClassA",
						singletonList(new PrioritizableTest("TestClassA/impactedTestCaseA()"))),
				new PrioritizableTestCluster("IgnoredTestClass",
						asList(new PrioritizableTest("IgnoredTestClass/ignoredTestClassTestCase1()"),
								new PrioritizableTest("IgnoredTestClass/ignoredTestClassTestCase2()")))));

		internalImpactedTestEngine.execute(executionRequest);

		// Start of engines.
		verify(executionListener).executionStarted(impactedTestEngineDescriptor);
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.testEngineRoot);

		// Execute TestClassA.
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.testClassA);
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.impactedTestCaseA);
		verify(executionListener)
				.executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.impactedTestCaseA), any());
		verify(executionListener)
				.executionSkipped(eq(ImpactedTestsSetup.InitialDiscovery.nonImpactedTestCaseA), any());
		verify(executionListener).executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.testClassA), any());

		// Execute IgnoredTestClass.
		verify(executionListener)
				.executionStarted(ImpactedTestsSetup.IgnoredTestClassDiscovery.ignoredTestClass);
		verify(executionListener)
				.executionSkipped(eq(ImpactedTestsSetup.IgnoredTestClassDiscovery.ignoredTestClassTestCase1), any());
		verify(executionListener)
				.executionSkipped(eq(ImpactedTestsSetup.IgnoredTestClassDiscovery.ignoredTestClassTestCase2), any());
		verify(executionListener)
				.executionFinished(ImpactedTestsSetup.IgnoredTestClassDiscovery.ignoredTestClass, successful());

		// Finish test engines.
		verify(executionListener)
				.executionFinished(ImpactedTestsSetup.InitialDiscovery.testEngineRoot, successful());
		verify(executionListener).executionFinished(impactedTestEngineDescriptor, successful());

		// Ensure test data is written.
		verify(testDataWriter).dumpTestDetails(any());
		verify(testDataWriter).dumpTestExecutions(any());

		verifyNoMoreInteractions(executionListener);
		verifyNoMoreInteractions(testDataWriter);
	}

	/** Test setup for {@link #impactedTestsAreExecutedCorrectly()}. */
	private static class ImpactedTestsSetup {

		/**
		 * For this test setup we rely on the {@link JUnitJupiterTestDescriptorResolver} for resolving uniform paths and
		 * cluster ids. Therefore the engine root is set accordingly.
		 */
		private static final UniqueId engineRootId = UniqueId.forEngine("junit-jupiter");

		/** TestClassA contains a one impacted and one non-impacted test. */
		private static final UniqueId testClassAId = engineRootId.append("class", "TestClassA");
		private static final UniqueId impactedTestCaseAId = testClassAId.append("method", "impactedTestCaseA()");
		private static final UniqueId nonImpactedTestCaseAId = testClassAId.append("method", "nonImpactedTestCaseA()");

		/**
		 * IgnoredTestClass is ignored (e.g. class is annotated with {@link Disabled}). Hence it'll be impacted since it
		 * was previously skipped.
		 */
		private static final UniqueId ignoredTestClassId =
				engineRootId.append("class", "IgnoredTestClass");
		private static final UniqueId ignoredTestClassTestCase1Id =
				ignoredTestClassId.append("method", "ignoredTestClassTestCase1()");
		private static final UniqueId ignoredTestClassTestCase2Id =
				ignoredTestClassId.append("method", "ignoredTestClassTestCase2()");

		/** Initial discovery of tests which includes all available tests for execution. */
		private static class InitialDiscovery {
			private static final TestDescriptor impactedTestCaseA = testCase(impactedTestCaseAId);
			private static final TestDescriptor nonImpactedTestCaseA = testCase(nonImpactedTestCaseAId);
			private static final TestDescriptor testClassA = testContainer(testClassAId, impactedTestCaseA,
					nonImpactedTestCaseA);

			private static final TestDescriptor ignoredTestClassTestCase1 = testCase(ignoredTestClassTestCase1Id);
			private static final TestDescriptor ignoredTestClassTestCase2 = testCase(ignoredTestClassTestCase2Id);
			private static final TestDescriptor ignoredTestClass = testContainer(ignoredTestClassId,
					ignoredTestClassTestCase1, ignoredTestClassTestCase2);

			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, testClassA,
					ignoredTestClass);
		}

		/** Discovery when executing {@link #impactedTestCaseAId} which is impacted. */
		private static class TestClassADiscovery {
			private static final TestDescriptor impactedTestCaseA = testCase(impactedTestCaseAId);
			private static final TestDescriptor testClassA = testContainer(testClassAId, impactedTestCaseA);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, testClassA);
		}

		/**
		 * Discovery when executing both previously skipped and therefore impacted tests cases {@link
		 * #ignoredTestClassTestCase1Id} and {@link #ignoredTestClassTestCase2Id}.
		 */
		private static class IgnoredTestClassDiscovery {
			private static final TestDescriptor ignoredTestClassTestCase1 = testCase(ignoredTestClassTestCase1Id);
			private static final TestDescriptor ignoredTestClassTestCase2 = testCase(ignoredTestClassTestCase2Id);
			private static final TestDescriptor ignoredTestClass = testContainer(ignoredTestClassId,
					ignoredTestClassTestCase1, ignoredTestClassTestCase2);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, ignoredTestClass);
		}
	}
}