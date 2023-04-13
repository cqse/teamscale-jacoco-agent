package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.TestExecution;

import java.util.List;

/** Interface for implementing different ways of executing tests. */
public interface ITestExecutor {

	/**
	 * The {@link TestExecution}s that have been tracked. May be empty if no {@link TestExecution}s are recorded by the
	 * {@link ITestExecutor}.
	 */
	List<TestExecution> execute(TestExecutorRequest testExecutorRequest);

}
