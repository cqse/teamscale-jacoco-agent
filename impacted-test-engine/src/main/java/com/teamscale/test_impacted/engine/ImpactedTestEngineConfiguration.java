package com.teamscale.test_impacted.engine;

import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier;
import org.junit.platform.engine.TestEngine;

/** Container for a configuration used by the {@link ImpactedTestEngine} */
public class ImpactedTestEngineConfiguration {

	/** The directory to write testwise coverage and available tests to. */
	final TestDataWriter testDataWriter;

	/** The test engine registry used to determine the {@link TestEngine}s to use. */
	final TestEngineRegistry testEngineRegistry;

	/** The {@link ITestExecutor} to use for execution of tests. */
	final ITestExecutor testExecutor;

	/** An API to signal test start and end to the agent. */
	final TeamscaleAgentNotifier teamscaleAgentNotifier;

	public ImpactedTestEngineConfiguration(
			TestDataWriter testDataWriter,
			TestEngineRegistry testEngineRegistry,
			ITestExecutor testExecutor, TeamscaleAgentNotifier teamscaleAgentNotifier) {
		this.testDataWriter = testDataWriter;
		this.testEngineRegistry = testEngineRegistry;
		this.testExecutor = testExecutor;
		this.teamscaleAgentNotifier = teamscaleAgentNotifier;
	}
}