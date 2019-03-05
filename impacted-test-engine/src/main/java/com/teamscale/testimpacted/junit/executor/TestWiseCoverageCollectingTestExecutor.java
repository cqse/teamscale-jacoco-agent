package com.teamscale.testimpacted.junit.executor;

import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.testimpacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.testimpacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.testimpacted.test_descriptor.TestDescriptorResolverRegistry;
import org.junit.platform.engine.ExecutionRequest;

import java.util.List;

public class TestWiseCoverageCollectingTestExecutor implements ITestExecutor {

	private List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis;

	public TestWiseCoverageCollectingTestExecutor(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis) {
		this.testwiseCoverageAgentApis = testwiseCoverageAgentApis;
	}

	@Override
	public List<TestExecution> execute(TestExecutorRequest testExecutorRequest) {
		ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverRegistry
				.getTestDescriptorResolver(testExecutorRequest.testEngine);
		DelegatingExecutionListener executionListener = new DelegatingExecutionListener(testwiseCoverageAgentApis,
				testDescriptorResolver, testExecutorRequest.engineExecutionListener);

		testExecutorRequest.testEngine.execute(new ExecutionRequest(testExecutorRequest.engineTestDescriptor,
				executionListener, testExecutorRequest.configurationParameters));

		return executionListener.getTestExecutions();
	}
}
