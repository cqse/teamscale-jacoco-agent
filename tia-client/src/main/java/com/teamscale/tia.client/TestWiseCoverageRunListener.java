package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * {@link RunListener} that uses the {@link TiaAgent} to record test-wise coverage.
 */
@SuppressWarnings("unused")
public class TestWiseCoverageRunListener extends RunListener {

	private final RunListenerAgentBridge bridge = new RunListenerAgentBridge(
			TestWiseCoverageRunListener.class.getName());

	@Override
	public void testStarted(Description description) {
		String uniformPath = getUniformPath(description);
		bridge.testStarted(uniformPath);
	}

	private String getUniformPath(Description description) {
		String uniformPath = description.getClassName().replace('.', '/');
		if (description.getMethodName() != null) {
			uniformPath += "/" + description.getMethodName();
		}
		return uniformPath;
	}

	@Override
	public void testFinished(Description description) {
		String uniformPath = getUniformPath(description);
		bridge.testFinished(uniformPath, ETestExecutionResult.PASSED);
	}

	@Override
	public void testFailure(Failure failure) {
		String uniformPath = getUniformPath(failure.getDescription());
		bridge.testFinished(uniformPath, ETestExecutionResult.FAILURE, failure.getMessage());
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		String uniformPath = getUniformPath(failure.getDescription());
		bridge.testFinished(uniformPath, ETestExecutionResult.FAILURE);
	}

	@Override
	public void testIgnored(Description description) {
		String uniformPath = getUniformPath(description);
		bridge.testSkipped(uniformPath, null);
	}

	@Override
	public void testRunFinished(Result result) {
		bridge.testRunFinished();
	}
}
