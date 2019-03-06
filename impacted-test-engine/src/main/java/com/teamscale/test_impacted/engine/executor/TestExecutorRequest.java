package com.teamscale.test_impacted.engine.executor;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

/** Wrapper class for an execution request passed to a {@link ITestExecutor}. */
public class TestExecutorRequest {

	/** The test engine to used to execute tests. */
	public final TestEngine testEngine;

	/** The root engine {@link TestDescriptor} containing all discovered tests. */
	public final TestDescriptor engineTestDescriptor;

	/** The execution listener to pass test events to. */
	public final EngineExecutionListener engineExecutionListener;

	/** Configuration parameters. */
	public final ConfigurationParameters configurationParameters;

	public TestExecutorRequest(TestEngine testEngine, TestDescriptor engineTestDescriptor, EngineExecutionListener engineExecutionListener, ConfigurationParameters configurationParameters) {
		this.testEngine = testEngine;
		this.engineTestDescriptor = engineTestDescriptor;
		this.engineExecutionListener = engineExecutionListener;
		this.configurationParameters = configurationParameters;
	}
}
