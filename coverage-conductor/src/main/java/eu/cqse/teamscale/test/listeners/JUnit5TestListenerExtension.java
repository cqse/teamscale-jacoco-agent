package eu.cqse.teamscale.test.listeners;

import eu.cqse.teamscale.test.controllers.JaCoCoAgentController;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class JUnit5TestListenerExtension implements TestExecutionListener {

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.getType() == TestDescriptor.Type.TEST) {
			JaCoCoAgentController.getInstance().onTestStart(testIdentifier.getUniqueId());
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testIdentifier.getType() == TestDescriptor.Type.TEST) {
			JaCoCoAgentController.getInstance().onTestFinish(testIdentifier.getUniqueId());
		}
	}
}
