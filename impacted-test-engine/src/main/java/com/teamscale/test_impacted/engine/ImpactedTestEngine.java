package com.teamscale.test_impacted.engine;

import com.teamscale.client.TestDetails;
import com.teamscale.report.ReportUtils;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.engine.executor.AvailableTests;
import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.TestExecutorRequest;
import com.teamscale.test_impacted.engine.options.OptionsUtils;
import com.teamscale.test_impacted.engine.options.TestEngineOptions;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImpactedTestEngine implements TestEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestEngine.class);

	static final String ENGINE_ID = "teamscale-test-impacted";

	private final TestEngineRegistry testEngineRegistry = new TestEngineRegistry();

	private final TestEngineOptions testEngineOptions = OptionsUtils.getEngineOptions(System.getProperties());

	@Override
	public String getId() {
		return ENGINE_ID;
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
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

	@Override
	public void execute(ExecutionRequest request) {
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
		ITestExecutor testExecutor = testEngineOptions.createTestExecutor();

		for (TestDescriptor engineTestDescriptor : request.getRootTestDescriptor().getChildren()) {
			Optional<String> engineId = engineTestDescriptor.getUniqueId().getEngineId();

			if (!engineId.isPresent()) {
				continue;
			}

			TestEngine testEngine = testEngineRegistry.getTestEngine(engineId.get());
			AvailableTests availableTestsForEngine = TestDescriptorUtils
					.getAvailableTests(testEngine, engineTestDescriptor);
			TestExecutorRequest testExecutorRequest = new TestExecutorRequest(testEngine, engineTestDescriptor,
					request.getEngineExecutionListener(), request.getConfigurationParameters());
			List<TestExecution> testExecutionsOfEngine = testExecutor.execute(testExecutorRequest);

			testExecutions.addAll(testExecutionsOfEngine);
			availableTests.addAll(availableTestsForEngine.getTestList());
		}

		dumpTestDetails(availableTests);
		dumpTestExecutions(testExecutions);
	}

	private void dumpTestExecutions(List<TestExecution> testExecutions) {
		writeReport(new File(testEngineOptions.getReportDirectory(), "test-execution.json"), testExecutions);
	}

	/** Writes the given test details to a report file. */
	private void dumpTestDetails(List<TestDetails> testDetails) {
		writeReport(new File(testEngineOptions.getReportDirectory(), "test-list.json"), testDetails);
	}

	private static <T> void writeReport(File file, T report) {
		try {
			ReportUtils.writeReportToFile(file, report);
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error while writing report to file: " + file);
		}
	}
}
