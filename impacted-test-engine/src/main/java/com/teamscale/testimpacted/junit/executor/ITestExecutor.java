package com.teamscale.testimpacted.junit.executor;

import com.teamscale.report.testwise.model.TestExecution;

import java.util.List;

public interface ITestExecutor {

	List<TestExecution> execute(TestExecutorRequest testExecutorRequest);
}
