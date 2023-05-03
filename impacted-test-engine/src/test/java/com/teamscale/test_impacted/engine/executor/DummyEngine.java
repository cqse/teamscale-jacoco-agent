package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

/**
 * A test engine that simulates the behavior of the vintage and jupiter engine that the impacted test engine invokes
 * under the hood.
 */
public class DummyEngine implements TestEngine {

	private final TestDescriptor descriptor;

	public DummyEngine(TestDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public String getId() {
		return descriptor.getUniqueId().getEngineId().get();
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		return descriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener executionListener = request.getEngineExecutionListener();
		this.executeDescriptor(executionListener, request.getRootTestDescriptor());
	}

	/**
	 * Calls the {@link EngineExecutionListener} callbacks in the expected order. The information whether tests should
	 * be skipped, should fail or have dynamic executions is attached to the {@link SimpleTestDescriptor}.
	 */
	private void executeDescriptor(EngineExecutionListener executionListener, TestDescriptor testDescriptor) {
		if (!(testDescriptor instanceof SimpleTestDescriptor)) {
			throw new IllegalArgumentException("Expected TestDescriptor to be of type SimpleTestDescriptor");
		}
		SimpleTestDescriptor simpleTestDescriptor = (SimpleTestDescriptor) testDescriptor;
		if (simpleTestDescriptor.shouldBeSkipped()) {
			executionListener.executionSkipped(testDescriptor, "Tests class is disabled.");
			return;
		}
		executionListener.executionStarted(testDescriptor);
		for (TestDescriptor child : testDescriptor.getChildren()) {
			executeDescriptor(executionListener, child);
		}
		for (TestDescriptor dynamicTest : simpleTestDescriptor.getDynamicTests()) {
			testDescriptor.addChild(dynamicTest);
			executionListener.dynamicTestRegistered(dynamicTest);
			executeDescriptor(executionListener, dynamicTest);
		}
		TestExecutionResult executionResult = simpleTestDescriptor.getExecutionResult();
		executionListener.executionFinished(testDescriptor, executionResult);
	}
}
