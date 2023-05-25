package com.teamscale.test_impacted.engine;

import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsProvider;
import com.teamscale.test_impacted.engine.executor.ImpactedTestsSorter;
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Base class for testing specific scenarios in the impacted test engine. */
public abstract class ImpactedTestEngineTestBase {

	private final TestEngineRegistry testEngineRegistry = mock(TestEngineRegistry.class);

	private final TestDataWriter testDataWriter = mock(TestDataWriter.class);

	private final ImpactedTestsProvider impactedTestsProvider = mock(ImpactedTestsProvider.class);

	private final EngineDiscoveryRequest discoveryRequest = mock(EngineDiscoveryRequest.class);

	private final ExecutionRequest executionRequest = mock(ExecutionRequest.class);

	private final EngineExecutionListener executionListener = mock(EngineExecutionListener.class);

	private final TeamscaleAgentNotifier teamscaleAgentNotifier = mock(TeamscaleAgentNotifier.class);

	@Test
	void testEngineExecution() {
		InternalImpactedTestEngine internalImpactedTestEngine = createInternalImpactedTestEngine(
				getEngines());

		TestDescriptor impactedTestEngineDescriptor = internalImpactedTestEngine
				.discover(discoveryRequest, UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));
		assertThat(impactedTestEngineDescriptor.getUniqueId())
				.isEqualTo(UniqueId.forEngine(ImpactedTestEngine.ENGINE_ID));

		when(executionRequest.getEngineExecutionListener()).thenReturn(executionListener);
		when(executionRequest.getRootTestDescriptor()).thenReturn(impactedTestEngineDescriptor);
		when(impactedTestsProvider.getImpactedTestsFromTeamscale(any())).thenReturn(
				getImpactedTests());

		internalImpactedTestEngine.execute(executionRequest);

		verifyCallbacks(executionListener);

		// Ensure test data is written.
		verify(testDataWriter).dumpTestDetails(any());
		verify(testDataWriter).dumpTestExecutions(any());

		verifyNoMoreInteractions(executionListener);
		verifyNoMoreInteractions(testDataWriter);
	}

	/** Returns the available engines that should be assumed by the impacted test engine. */
	abstract List<TestEngine> getEngines();

	/** Returns the result that Teamscale should return when asked for impacted tests. */
	abstract List<PrioritizableTestCluster> getImpactedTests();

	/** Verifies that the interactions with the executionListener are the ones we would expect. */
	abstract void verifyCallbacks(EngineExecutionListener executionListener);

	private InternalImpactedTestEngine createInternalImpactedTestEngine(List<TestEngine> engines) {
		for (TestEngine engine : engines) {
			when(testEngineRegistry.getTestEngine(eq(engine.getId()))).thenReturn(engine);
		}
		when(testEngineRegistry.iterator()).thenReturn(engines.iterator());

		return new InternalImpactedTestEngine(
				new ImpactedTestEngineConfiguration(testDataWriter, testEngineRegistry,
						new ImpactedTestsSorter(impactedTestsProvider),
						teamscaleAgentNotifier),
				impactedTestsProvider.partition);
	}
}
