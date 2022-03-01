package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Optional;

/**
 * {@link TestExecutionListener} that uses the {@link TiaAgent} to record test-wise coverage.
 */
@SuppressWarnings("unused")
public class JUnit5TestWiseCoverageExecutionListener implements TestExecutionListener {

	private final RunListenerAgentBridge bridge = new RunListenerAgentBridge(
			JUnit5TestWiseCoverageExecutionListener.class.getName());

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (!testIdentifier.isTest()) {
			return;
		}
		String uniformPath = getUniformPath(testIdentifier);
		bridge.testStarted(uniformPath);
	}

	private String getUniformPath(TestIdentifier testIdentifier) {
		return testIdentifier.getSource().flatMap(this::parseTestSource).orElse(testIdentifier.getDisplayName());
	}

	private Optional<String> parseTestSource(TestSource source) {
		if (source instanceof ClassSource) {
			ClassSource classSource = (ClassSource) source;
			return Optional.of(classSource.getClassName().replace('.', '/'));
		} else if (source instanceof MethodSource) {
			MethodSource methodSource = (MethodSource) source;
			return Optional.of(methodSource.getClassName().replace('.', '/') + "/" +
					methodSource.getMethodName() + "(" + methodSource.getMethodParameterTypes() + ")");
		}
		return Optional.empty();
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (!testIdentifier.isTest()) {
			return;
		}
		String uniformPath = getUniformPath(testIdentifier);
		ETestExecutionResult result;
		switch (testExecutionResult.getStatus()) {
			case SUCCESSFUL:
				result = ETestExecutionResult.PASSED;
				break;
			case ABORTED:
				result = ETestExecutionResult.ERROR;
				break;
			case FAILED:
			default:
				result = ETestExecutionResult.FAILURE;
				break;
		}

		bridge.testFinished(uniformPath, result);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isContainer()) {
			return;
		}
		String uniformPath = getUniformPath(testIdentifier);
		bridge.testSkipped(uniformPath, reason);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		bridge.testRunFinished();
	}
}
