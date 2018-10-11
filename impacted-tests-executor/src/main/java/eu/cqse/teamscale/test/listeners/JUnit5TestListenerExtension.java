package eu.cqse.teamscale.test.listeners;

import eu.cqse.teamscale.report.testwise.model.ETestExecutionResult;
import eu.cqse.teamscale.report.testwise.model.TestExecution;
import eu.cqse.teamscale.test.controllers.ITestwiseCoverageAgentApi;
import okhttp3.HttpUrl;
import org.junit.platform.console.Logger;
import org.junit.platform.console.tasks.TestDetailsCollector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.util.ArrayList;
import java.util.List;

/** Implementation of the {@link TestExecutionListener} interface provided by the JUnit platform. */
public class JUnit5TestListenerExtension implements TestExecutionListener {

	private ITestwiseCoverageAgentApi controller;

	public List<TestExecution> getTestExecutions() {
		return testExecutions;
	}

	private List<TestExecution> testExecutions = new ArrayList<>();

	private Logger logger;

	public JUnit5TestListenerExtension(HttpUrl url, Logger logger) {
		this.controller = ITestwiseCoverageAgentApi.createService(url);
		this.logger = logger;
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.isTest()) {
			String testUniformPath = TestDetailsCollector.getTestUniformPath(testIdentifier, logger);
			controller.testStarted(testUniformPath);
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testIdentifier.isTest()) {
			String testUniformPath = TestDetailsCollector.getTestUniformPath(testIdentifier, logger);
			controller.testFinished(testUniformPath);
			if(testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
				testExecutions.add(new TestExecution( testUniformPath, testExecutionResult., ETestExecutionResult.PASSED));
			}
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isTest()) {
			String testUniformPath = TestDetailsCollector.getTestUniformPath(testIdentifier, logger);
			testExecutions.add(new TestExecution( testUniformPath, 0, ETestExecutionResult.SKIPPED, reason));
		}
	}
}
