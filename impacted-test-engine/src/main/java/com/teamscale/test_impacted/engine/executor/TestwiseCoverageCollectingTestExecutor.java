package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorResolverRegistry;
import org.junit.platform.engine.ExecutionRequest;

import java.util.List;

/** Test executor that records test wise coverage and executes the full {@link TestExecutorRequest}. */
public class TestwiseCoverageCollectingTestExecutor implements ITestExecutor {

	private List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis;

	public TestwiseCoverageCollectingTestExecutor(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis) {
		this.testwiseCoverageAgentApis = testwiseCoverageAgentApis;
	}

	@Override
	public List<TestExecution> execute(TestExecutorRequest testExecutorRequest) {
		ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverRegistry
				.getTestDescriptorResolver(testExecutorRequest.testEngine);
		TestwiseCoverageCollectingExecutionListener executionListener = new TestwiseCoverageCollectingExecutionListener(
				testwiseCoverageAgentApis,
				testDescriptorResolver, testExecutorRequest.engineExecutionListener);

		testExecutorRequest.testEngine.execute(new ExecutionRequest(testExecutorRequest.engineTestDescriptor,
				executionListener, testExecutorRequest.configurationParameters));

		return executionListener.getTestExecutions();
	}
}
