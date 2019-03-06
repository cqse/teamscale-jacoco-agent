package com.teamscale.testimpacted.junit.executor;

import com.teamscale.report.testwise.model.TestExecution;
import org.junit.platform.engine.ExecutionRequest;

import java.util.Collections;
import java.util.List;

/** Simple test executor that does nothing but execute the whole {@link TestExecutorRequest}. */
public class DelegatingTestExecutor implements ITestExecutor {

	@Override
	public List<TestExecution> execute(TestExecutorRequest testExecutorRequest) {
		testExecutorRequest.testEngine.execute(new ExecutionRequest(testExecutorRequest.engineTestDescriptor,
				testExecutorRequest.engineExecutionListener, testExecutorRequest.configurationParameters));
		return Collections.emptyList();
	}
}

