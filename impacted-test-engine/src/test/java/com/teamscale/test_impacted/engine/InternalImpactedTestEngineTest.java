package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsExecutor;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsProvider;
import com.teamscale.test_impacted.test_descriptor.JUnitJupiterTestDescriptorResolver;
import com.teamscale.tia.ITestwiseCoverageAgentApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import retrofit2.Call;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testCase;
import static com.teamscale.test_impacted.engine.executor.SimpleTestDescriptor.testContainer;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

	private final ImpactedTestsProvider impactedTestsProvider = mock(ImpactedTestsProvider.class);

	private final EngineDiscoveryRequest discoveryRequest = mock(EngineDiscoveryRequest.class);

	private final ExecutionRequest executionRequest = mock(ExecutionRequest.class);

	private final EngineExecutionListener executionListener = mock(EngineExecutionListener.class);

	private final ITestwiseCoverageAgentApi testwiseCoverageAgentApi = mock(ITestwiseCoverageAgentApi.class);

	private InternalImpactedTestEngine createInternalImpactedTestEngine(ITestExecutor testExecutor) {
		return new InternalImpactedTestEngine(testEngineRegistry, testExecutor, testDataWriter);
	}

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setupTestEngineAndCoverageAgent() {
		when(testEngineRegistry.getTestEngine(any())).thenReturn(testEngine);
		when(testEngineRegistry.iterator()).thenReturn(singletonList(testEngine).iterator());
		when(testEngine.getId()).thenReturn("junit-jupiter");
		when(testwiseCoverageAgentApi.testStarted(anyString())).thenReturn(mock(Call.class));
		when(testwiseCoverageAgentApi.testFinished(anyString(), any())).thenReturn(mock(Call.class));
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
				() -> ImpactedTestsSetup.FirstTestClassDiscovery.testEngineRoot,
				() -> ImpactedTestsSetup.IgnoredTestClassDiscovery.testEngineRoot,
				() -> ImpactedTestsSetup.SecondTestClassDiscovery.testEngineRoot);
		TestExecutionResult failed = failed(new NullPointerException());
		setupTestEngineExecutions(executionListener -> {
			executionListener.executionStarted(ImpactedTestsSetup.FirstTestClassDiscovery.testEngineRoot);
			executionListener.executionStarted(
					ImpactedTestsSetup.FirstTestClassDiscovery.firstTestClass);
			executionListener
					.executionStarted(ImpactedTestsSetup.FirstTestClassDiscovery.impactedTestCase1);
			executionListener
					.executionFinished(ImpactedTestsSetup.FirstTestClassDiscovery.impactedTestCase1,
							successful());
			executionListener.executionFinished(
					ImpactedTestsSetup.FirstTestClassDiscovery.firstTestClass, successful());
			executionListener.executionFinished(ImpactedTestsSetup.FirstTestClassDiscovery.testEngineRoot,
					successful());
		}, executionListener -> {
			executionListener.executionStarted(ImpactedTestsSetup.IgnoredTestClassDiscovery.testEngineRoot);
			executionListener.executionSkipped(ImpactedTestsSetup.IgnoredTestClassDiscovery.ignoredTestClass,
					"Tests class is disabled.");
			executionListener
					.executionFinished(ImpactedTestsSetup.IgnoredTestClassDiscovery.testEngineRoot, successful());
		}, executionListener -> {
			executionListener.executionStarted(ImpactedTestsSetup.SecondTestClassDiscovery.testEngineRoot);
			executionListener.executionStarted(
					ImpactedTestsSetup.SecondTestClassDiscovery.secondTestClass);
			executionListener.executionStarted(ImpactedTestsSetup.SecondTestClassDiscovery.impactedTestCase);
			executionListener.executionFinished(ImpactedTestsSetup.SecondTestClassDiscovery.impactedTestCase,
					failed);
			executionListener.executionSkipped(ImpactedTestsSetup.SecondTestClassDiscovery.skippedImpactedTestCase,
					"Test case is disabled.");
			executionListener.executionFinished(
					ImpactedTestsSetup.SecondTestClassDiscovery.secondTestClass, successful());
			executionListener.executionFinished(ImpactedTestsSetup.SecondTestClassDiscovery.testEngineRoot,
					successful());
		});

		TestDescriptor impactedTestEngineDescriptor = internalImpactedTestEngine
				.discover(discoveryRequest, UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));
		assertThat(impactedTestEngineDescriptor.getUniqueId())
				.isEqualTo(UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));

		when(executionRequest.getEngineExecutionListener()).thenReturn(executionListener);
		when(executionRequest.getRootTestDescriptor()).thenReturn(impactedTestEngineDescriptor);
		when(impactedTestsProvider.getImpactedTestsFromTeamscale(any())).thenReturn(
				createList(
						ImpactedTestsSetup.FirstTestClassDiscovery.getImpactedTestClusters(),
						ImpactedTestsSetup.IgnoredTestClassDiscovery.getImpactedTestClsuters(),
						ImpactedTestsSetup.SecondTestClassDiscovery.getImpactedTestClsuters()));

		internalImpactedTestEngine.execute(executionRequest);

		// Start of engines.
		verify(executionListener).executionStarted(impactedTestEngineDescriptor);
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.testEngineRoot);

		// Execute FirstTestClass.
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.firstTestClass);
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.impactedTestCase1);
		verify(executionListener).executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.impactedTestCase1), any());
		verify(executionListener).executionSkipped(eq(ImpactedTestsSetup.InitialDiscovery.nonImpactedTestCase1), any());
		verify(executionListener).executionFinished(eq(ImpactedTestsSetup.InitialDiscovery.firstTestClass), any());

		// Execute IgnoredTestClass.
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.ignoredTestClass);
		verify(executionListener).executionSkipped(eq(ImpactedTestsSetup.InitialDiscovery.impactedTestCase2), any());
		verify(executionListener).executionSkipped(eq(ImpactedTestsSetup.InitialDiscovery.nonImpactedTestCase2), any());
		verify(executionListener).executionFinished(ImpactedTestsSetup.InitialDiscovery.ignoredTestClass, successful());

		// Execute SecondTestClass.
		verify(executionListener).executionStarted(ImpactedTestsSetup.InitialDiscovery.secondTestClass);
		verify(executionListener).executionStarted(eq(ImpactedTestsSetup.InitialDiscovery.impactedTestCase3));
		verify(executionListener).executionFinished(ImpactedTestsSetup.InitialDiscovery.impactedTestCase3, failed);
		verify(executionListener)
				.executionSkipped(eq(ImpactedTestsSetup.InitialDiscovery.skippedImpactedTestCase), any());
		verify(executionListener).executionFinished(ImpactedTestsSetup.InitialDiscovery.secondTestClass, successful());

		// Finish test engines.
		verify(executionListener).executionFinished(ImpactedTestsSetup.InitialDiscovery.testEngineRoot, successful());
		verify(executionListener).executionFinished(impactedTestEngineDescriptor, successful());

		// Ensure test data is written.
		verify(testDataWriter).dumpTestDetails(any());
		verify(testDataWriter).dumpTestExecutions(any());

		verifyNoMoreInteractions(executionListener);
		verifyNoMoreInteractions(testDataWriter);
	}

	@SafeVarargs
	private final List<PrioritizableTestCluster> createList(List<PrioritizableTestCluster>... impactedTestClusters) {
		return Arrays.stream(impactedTestClusters).flatMap(Collection::stream).collect(Collectors.toList());
	}

	/** Test setup for {@link #impactedTestsAreExecutedCorrectly()}. */
	private static class ImpactedTestsSetup {

		/**
		 * For this test setup we rely on the {@link JUnitJupiterTestDescriptorResolver} for resolving uniform paths and
		 * cluster ids. Therefore the engine root is set accordingly.
		 */
		private static final UniqueId engineRootId = UniqueId.forEngine("junit-jupiter");

		/** FirstTestClassDiscovery contains a one impacted and one non-impacted test. */
		private static final UniqueId firstTestClassId = engineRootId.append("class", "FirstTestClass");
		private static final UniqueId impactedTestCase1Id = firstTestClassId.append("method", "impactedTestCase1()");
		private static final UniqueId nonImpactedTestCase1Id = firstTestClassId
				.append("method", "nonImpactedTestCase1()");

		/**
		 * IgnoredTestClass is ignored (e.g. class is annotated with {@link Disabled}). Hence it'll be impacted since it
		 * was previously skipped.
		 */
		private static final UniqueId ignoredTestClassId = engineRootId.append("class", "IgnoredTestClass");
		private static final UniqueId impactedTestCase2Id = ignoredTestClassId.append("method", "impactedTestCase2()");
		private static final UniqueId nonImpactedTestCase2Id = ignoredTestClassId
				.append("method", "nonImpactedTestCase2()");

		/**
		 * ImpactedTestClassWithSkippedTest contains two impacted tests of which one is skipped.
		 */
		private static final UniqueId secondTestClassId = engineRootId.append("class", "SecondTestClass");
		private static final UniqueId impactedTestCase3Id = secondTestClassId.append("method", "impactedTestCase3()");
		private static final UniqueId skippedImpactedTestCaseId = secondTestClassId
				.append("method", "skippedImpactedTestCaseId()");

		/** Initial discovery of tests which includes all available tests for execution. */
		private static class InitialDiscovery {
			private static final TestDescriptor impactedTestCase1 = testCase(impactedTestCase1Id);
			private static final TestDescriptor nonImpactedTestCase1 = testCase(nonImpactedTestCase1Id);
			private static final TestDescriptor firstTestClass = testContainer(firstTestClassId,
					impactedTestCase1, nonImpactedTestCase1);

			private static final TestDescriptor impactedTestCase2 = testCase(impactedTestCase2Id);
			private static final TestDescriptor nonImpactedTestCase2 = testCase(nonImpactedTestCase2Id);
			private static final TestDescriptor ignoredTestClass = testContainer(ignoredTestClassId,
					impactedTestCase2, nonImpactedTestCase2);

			private static final TestDescriptor impactedTestCase3 = testCase(impactedTestCase3Id);
			private static final TestDescriptor skippedImpactedTestCase = testCase(skippedImpactedTestCaseId);
			private static final TestDescriptor secondTestClass = testContainer(secondTestClassId,
					impactedTestCase3, skippedImpactedTestCase);

			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, firstTestClass,
					ignoredTestClass, secondTestClass);
		}

		/** Discovery when executing {@link #impactedTestCase1Id} which is impacted. */
		private static class FirstTestClassDiscovery {
			private static final TestDescriptor impactedTestCase1 = testCase(impactedTestCase1Id);
			private static final TestDescriptor firstTestClass = testContainer(firstTestClassId, impactedTestCase1);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, firstTestClass);

			private static List<PrioritizableTestCluster> getImpactedTestClusters() {
				return singletonList(new PrioritizableTestCluster("FirstTestClass",
						singletonList(new PrioritizableTest("FirstTestClass/impactedTestCase1()"))));
			}
		}

		/**
		 * For the IgnoredClass only the test case {@link #impactedTestCase2Id} should be executed but the whole class
		 * is ignored.
		 */
		private static class IgnoredTestClassDiscovery {
			private static final TestDescriptor impactedTestCase2 = testCase(impactedTestCase2Id);
			private static final TestDescriptor ignoredTestClass = testContainer(ignoredTestClassId, impactedTestCase2);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, ignoredTestClass);

			private static List<PrioritizableTestCluster> getImpactedTestClsuters() {
				return singletonList(new PrioritizableTestCluster("IgnoredTestClass",
						singletonList(new PrioritizableTest("IgnoredTestClass/impactedTestCase2()"))));
			}
		}

		/**
		 * Discovery when executing both previously skipped and therefore impacted tests cases {@link
		 * #impactedTestCase3Id} and {@link #impactedTestCase3Id}.
		 */
		private static class SecondTestClassDiscovery {
			private static final TestDescriptor impactedTestCase = testCase(impactedTestCase3Id);
			private static final TestDescriptor skippedImpactedTestCase = testCase(skippedImpactedTestCaseId);
			private static final TestDescriptor secondTestClass = testContainer(secondTestClassId, impactedTestCase,
					skippedImpactedTestCase);
			private static final TestDescriptor testEngineRoot = testContainer(engineRootId, secondTestClass);


			private static List<PrioritizableTestCluster> getImpactedTestClsuters() {
				return singletonList(new PrioritizableTestCluster("SecondTestClass",
						asList(new PrioritizableTest("SecondTestClass/impactedTestCase3()"),
								new PrioritizableTest("SecondTestClass/skippedImpactedTestCaseId()"))));
			}
		}
	}
}