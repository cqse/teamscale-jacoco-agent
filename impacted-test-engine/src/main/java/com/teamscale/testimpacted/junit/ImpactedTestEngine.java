package com.teamscale.testimpacted.junit;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TestClusterForPrioritization;
import com.teamscale.testimpacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.testimpacted.junit.options.OptionsUtils;
import com.teamscale.testimpacted.junit.options.ServerOptions;
import com.teamscale.testimpacted.junit.options.TestEngineOptions;
import com.teamscale.testimpacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.testimpacted.test_descriptor.TestDescriptorResolverFactory;
import com.teamscale.testimpacted.test_descriptor.TestDescriptorUtils;
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
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

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
		List<TestEngine> enabledTestEngines = new ArrayList<>(testEngineRegistry.getEnabledTestEngines().values());

		enabledTestEngines.sort(Comparator.comparing(TestEngine::getId));

		LOGGER.debug(() -> "Starting test discovery for engine " + ENGINE_ID);

		for (TestEngine delegateTestEngine : enabledTestEngines) {
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
		Map<String, TestEngine> delegateTestEngines = testEngineRegistry.getEnabledTestEngines();
		List<ITestwiseCoverageAgentApi> apiServices = testEngineOptions.getAgentsUrls().stream()
				.map(ITestwiseCoverageAgentApi::createService).collect(toList());

		LOGGER.debug(() -> "Starting execution of request for engine " + ENGINE_ID + ":\n" + TestDescriptorUtils
				.getTestDescriptorAsString(engineDescriptor));

		engineExecutionListener.executionStarted(engineDescriptor);

		try {

			if (!testEngineOptions.isRunImpacted() || testEngineOptions.isRunAllTests()) {
				runAllTests(request, apiServices, delegateTestEngines);
			} else {
				runImpactedTests(request, apiServices, delegateTestEngines);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		engineExecutionListener.executionFinished(engineDescriptor, TestExecutionResult.successful());
	}

	private void runAllTests(ExecutionRequest request, List<ITestwiseCoverageAgentApi> apiServices, Map<String, TestEngine> delegateTestEngines) {
		for (TestDescriptor engineTestDescriptor : request.getRootTestDescriptor().getChildren()) {
			Optional<String> engineId = engineTestDescriptor.getUniqueId().getEngineId();
			if (!engineId.isPresent()) {
				continue;
			}
			TestEngine testEngine = delegateTestEngines.get(engineId.get());

			EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();

			if (testEngineOptions.isRunImpacted()) {
				engineExecutionListener = new DelegatingExecutionListener(apiServices,
						TestDescriptorResolverFactory.getTestDescriptorResolver(testEngine),
						engineExecutionListener);
			}

			testEngine.execute(new ExecutionRequest(engineTestDescriptor, engineExecutionListener,
					request.getConfigurationParameters()));
		}
	}

	private void runImpactedTests(ExecutionRequest request, List<ITestwiseCoverageAgentApi> apiServices, Map<String, TestEngine> delegateTestEngines) {
		AvailableTests availableTestDetails = getAvailableTestDetails(request.getRootTestDescriptor().getChildren());
		List<TestClusterForPrioritization> testClustersForPrioritization = getImpactedTestsFromTeamscale(
				availableTestDetails.getTestList());
		List<TestEngine> requestedEngines = request.getRootTestDescriptor().getChildren().stream()
				.map(TestDescriptor::getUniqueId)
				.map(UniqueId::getEngineId)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(delegateTestEngines::get)
				.filter(Objects::nonNull)
				.collect(toList());

		for (TestClusterForPrioritization testClusterForPrioritization : testClustersForPrioritization) {
			for (TestEngine requestedEngine : requestedEngines) {
				ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverFactory
						.getTestDescriptorResolver(requestedEngine);

				LOGGER.debug(() -> "Re-discovering tests for delegate engine " + requestedEngine.getId());

				UniqueIdsDiscoveryRequest engineDiscoveryRequest = new UniqueIdsDiscoveryRequest(
						availableTestDetails.convertToUniqueIds(testClusterForPrioritization.testsForPrioritization),
						request.getConfigurationParameters());
				TestDescriptor testDescriptor = requestedEngine.discover(engineDiscoveryRequest,
						UniqueId.forEngine(requestedEngine.getId()));

				LOGGER.debug(
						() -> "Executing tests for re-discovered test descriptor of engine " + requestedEngine
								.getId() + ":\n" + TestDescriptorUtils
								.getTestDescriptorAsString(testDescriptor));

				DelegatingExecutionListener delegateEngineExecutionListener = new DelegatingExecutionListener(
						apiServices,
						testDescriptorResolver,
						request.getEngineExecutionListener());
				ExecutionRequest executionRequest = new ExecutionRequest(testDescriptor,
						delegateEngineExecutionListener, request.getConfigurationParameters());

				requestedEngine.execute(executionRequest);
			}
		}
	}

	private AvailableTests getAvailableTestDetails(Set<? extends TestDescriptor> engineDescriptors) {
		AvailableTests availableTests = new AvailableTests();

		for (TestDescriptor engineDescriptor : engineDescriptors) {
			Optional<String> engineId = engineDescriptor.getUniqueId().getEngineId();

			if (!engineId.isPresent()) {
				continue;
			}

			ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverFactory
					.getTestDescriptorResolver(engineId.get());

			TestDescriptorUtils.streamLeafTestDescriptors(engineDescriptor)
					.forEach(testDescriptor ->
							testDescriptorResolver
									.toClusteredTestDetails(testDescriptor)
									.ifPresent(clusteredTestDetails ->
											availableTests.add(testDescriptor.getUniqueId(), clusteredTestDetails)
									));
		}

		return availableTests;
	}

	/** Queries Teamscale for impacted tests. */
	private List<TestClusterForPrioritization> getImpactedTestsFromTeamscale(List<ClusteredTestDetails> availableTestDetails) {
		try {
			LOGGER.info(() -> "Getting impacted tests...");
			ServerOptions serverOptions = testEngineOptions.getServerOptions();
			TeamscaleClient client = new TeamscaleClient(serverOptions.getUrl(), serverOptions.getUserName(),
					serverOptions.getUserAccessToken(), serverOptions.getProject());
			Response<List<TestClusterForPrioritization>> response = client
					.getImpactedTests(availableTestDetails, testEngineOptions.getBaseline(),
							testEngineOptions.getEndCommit(), testEngineOptions.getPartition());
			if (response.isSuccessful()) {
				List<TestClusterForPrioritization> testList = response.body();
				if (testList == null) {
					LOGGER.error(() -> "Teamscale was not able to determine impacted tests.");
				}
				return testList;
			} else {
				LOGGER.error(() -> "Retrieval of impacted tests failed");
				LOGGER.error(() -> response.code() + " " + response.message());
			}
		} catch (IOException e) {
			LOGGER.error(() -> "Retrieval of impacted tests failed (" + e.getMessage() + ")");
		}
		return Collections.emptyList();
	}
}
