package com.teamscale.testimpacted.junit.executor;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

public class TestExecutorRequest {

	public final TestEngine testEngine;

	public final TestDescriptor engineTestDescriptor;

	public final EngineExecutionListener engineExecutionListener;

	public final ConfigurationParameters configurationParameters;

	public TestExecutorRequest(TestEngine testEngine, TestDescriptor engineTestDescriptor, EngineExecutionListener engineExecutionListener, ConfigurationParameters configurationParameters) {
		this.testEngine = testEngine;
		this.engineTestDescriptor = engineTestDescriptor;
		this.engineExecutionListener = engineExecutionListener;
		this.configurationParameters = configurationParameters;
	}
}
