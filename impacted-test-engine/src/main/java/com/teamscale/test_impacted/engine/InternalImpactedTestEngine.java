package com.teamscale.test_impacted.engine;

import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.commons.LoggerUtils;
import com.teamscale.test_impacted.engine.executor.AvailableTests;
import com.teamscale.test_impacted.engine.executor.ITestSorter;
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier;
import com.teamscale.test_impacted.engine.executor.TestwiseCoverageCollectingExecutionListener;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorResolverRegistry;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import static com.teamscale.test_impacted.engine.ImpactedTestEngine.ENGINE_ID;

/**
 * Test engine called internally to allow testing without needing a {@link ServiceLoader} for {@link TestEngine} setup.
 */
class InternalImpactedTestEngine {

	private static final Logger LOGGER = LoggerUtils.getLogger(InternalImpactedTestEngine.class);

	private final TestEngineRegistry testEngineRegistry;

	private final ITestSorter testSorter;

	private final TeamscaleAgentNotifier teamscaleAgentNotifier;

	private final TestDataWriter testDataWriter;

	private final String partition;

	InternalImpactedTestEngine(ImpactedTestEngineConfiguration configuration, String partition) {
		this.testEngineRegistry = configuration.testEngineRegistry;
		this.testSorter = configuration.testSorter;
		this.testDataWriter = configuration.testDataWriter;
		this.teamscaleAgentNotifier = configuration.teamscaleAgentNotifier;
		this.partition = partition;
	}

	/**
	 * Performs test discovery by aggregating the result of all {@link TestEngine}s from the {@link TestEngineRegistry}
	 * in a single engine {@link TestDescriptor}.
	 */
	TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "Teamscale Impacted Tests");

		LOGGER.fine(() -> "Starting test discovery for engine " + ENGINE_ID);

		for (TestEngine delegateTestEngine : testEngineRegistry) {
			LOGGER.fine(() -> "Starting test discovery for delegate engine: " + delegateTestEngine.getId());
			TestDescriptor delegateEngineDescriptor = delegateTestEngine.discover(discoveryRequest,
					UniqueId.forEngine(delegateTestEngine.getId()));

			engineDescriptor.addChild(delegateEngineDescriptor);
		}

		LOGGER.fine(() -> "Discovered test descriptor for engine " + ENGINE_ID + ":\n" + TestDescriptorUtils
				.getTestDescriptorAsString(engineDescriptor));

		return engineDescriptor;
	}

	/**
	 * Executes the request by requesting execution of the {@link TestDescriptor} children aggregated in
	 * {@link #discover(EngineDiscoveryRequest, UniqueId)} with the corresponding {@link TestEngine}.
	 */
	void execute(ExecutionRequest request) {
		TestDescriptor rootTestDescriptor = request.getRootTestDescriptor();
		AvailableTests availableTests = TestDescriptorUtils.getAvailableTests(rootTestDescriptor, partition);

		LOGGER.fine(() -> "Starting selection and sorting " + ENGINE_ID + ":\n" + TestDescriptorUtils
				.getTestDescriptorAsString(rootTestDescriptor));

		testSorter.selectAndSort(rootTestDescriptor);

		LOGGER.fine(() -> "Starting execution of request for engine " + ENGINE_ID + ":\n" + TestDescriptorUtils
				.getTestDescriptorAsString(rootTestDescriptor));

		List<TestExecution> testExecutions = executeTests(request, rootTestDescriptor);

		testDataWriter.dumpTestExecutions(testExecutions);
		testDataWriter.dumpTestDetails(availableTests.getTestList());
		teamscaleAgentNotifier.testRunEnded();
	}

	private List<TestExecution> executeTests(ExecutionRequest request, TestDescriptor rootTestDescriptor) {
		List<TestExecution> testExecutions = new ArrayList<>();
		for (TestDescriptor engineTestDescriptor : rootTestDescriptor.getChildren()) {
			Optional<String> engineId = engineTestDescriptor.getUniqueId().getEngineId();
			if (!engineId.isPresent()) {
				LOGGER.severe(
						() -> "Engine ID for test descriptor " + engineTestDescriptor + " not present. Skipping execution of the engine.");
				continue;
			}

			TestEngine testEngine = testEngineRegistry.getTestEngine(engineId.get());
			ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverRegistry
					.getTestDescriptorResolver(testEngine.getId());
			TestwiseCoverageCollectingExecutionListener executionListener =
					new TestwiseCoverageCollectingExecutionListener(teamscaleAgentNotifier,
							testDescriptorResolver,
							request.getEngineExecutionListener());

			testEngine.execute(new ExecutionRequest(engineTestDescriptor, executionListener,
					request.getConfigurationParameters()));

			testExecutions.addAll(executionListener.getTestExecutions());
		}
		return testExecutions;
	}
}
