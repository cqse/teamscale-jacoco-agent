package com.teamscale.test_impacted.engine;

import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.commons.LoggerUtils;
import com.teamscale.test_impacted.engine.executor.AvailableTests;
import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier;
import com.teamscale.test_impacted.engine.executor.TestExecutorRequest;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
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

	private final ITestExecutor testExecutor;

	private final TeamscaleAgentNotifier teamscaleAgentNotifier;

	private final TestDataWriter testDataWriter;

	private final String partition;

	InternalImpactedTestEngine(ImpactedTestEngineConfiguration configuration, String partition) {
		this.testEngineRegistry = configuration.testEngineRegistry;
		this.testExecutor = configuration.testExecutor;
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

		LOGGER.debug(() -> "Starting test discovery for engine " + ENGINE_ID);

		for (TestEngine delegateTestEngine : testEngineRegistry) {
			LOGGER.debug(() -> "Starting test discovery for delegate engine: " + delegateTestEngine.getId());
			TestDescriptor delegateEngineDescriptor = delegateTestEngine.discover(discoveryRequest,
					UniqueId.forEngine(delegateTestEngine.getId()));

			engineDescriptor.addChild(delegateEngineDescriptor);
		}

		LOGGER.debug(() -> "Discovered test descriptor for engine " + ENGINE_ID + ":\n" + TestDescriptorUtils
				.getTestDescriptorAsString(engineDescriptor));

		return engineDescriptor;
	}

	/**
	 * Executes the request by requesting execution of the {@link TestDescriptor} children aggregated in
	 * {@link #discover(EngineDiscoveryRequest, UniqueId)} with the corresponding {@link TestEngine}.
	 */
	void execute(ExecutionRequest request) {
		EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
		EngineDescriptor engineDescriptor = (EngineDescriptor) request.getRootTestDescriptor();

		LOGGER.debug(() -> "Starting execution of request for engine " + ENGINE_ID + ":\n" + TestDescriptorUtils
				.getTestDescriptorAsString(engineDescriptor));

		engineExecutionListener.executionStarted(engineDescriptor);
		runTestExecutor(request);
		engineExecutionListener.executionFinished(engineDescriptor, TestExecutionResult.successful());
	}


	private void runTestExecutor(ExecutionRequest request) {
		List<TestDetails> availableTests = new ArrayList<>();
		List<TestExecution> testExecutions = new ArrayList<>();

		for (TestDescriptor engineTestDescriptor : request.getRootTestDescriptor().getChildren()) {
			Optional<String> engineId = engineTestDescriptor.getUniqueId().getEngineId();

			if (!engineId.isPresent()) {
				LOGGER.error(
						() -> "Engine id for test descriptor " + engineTestDescriptor + " not present. Skipping execution of the engine.");
				continue;
			}

			TestEngine testEngine = testEngineRegistry.getTestEngine(engineId.get());
			AvailableTests availableTestsForEngine = TestDescriptorUtils
					.getAvailableTests(testEngine, engineTestDescriptor, partition);
			TestExecutorRequest testExecutorRequest = new TestExecutorRequest(testEngine, engineTestDescriptor,
					request.getEngineExecutionListener(), teamscaleAgentNotifier, request.getConfigurationParameters());
			List<TestExecution> testExecutionsOfEngine = testExecutor.execute(testExecutorRequest);

			testExecutions.addAll(testExecutionsOfEngine);
			availableTests.addAll(availableTestsForEngine.getTestList());
		}

		testDataWriter.dumpTestDetails(availableTests);
		testDataWriter.dumpTestExecutions(testExecutions);
		teamscaleAgentNotifier.testRunEnded();
	}
}
