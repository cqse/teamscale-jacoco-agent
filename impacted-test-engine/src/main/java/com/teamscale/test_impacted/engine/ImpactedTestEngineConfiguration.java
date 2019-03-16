package com.teamscale.test_impacted.engine;

import com.teamscale.test_impacted.engine.executor.ITestExecutor;
import org.junit.platform.engine.TestEngine;

import java.io.File;

/** Container for a configuration used by the {@link ImpactedTestEngine} */
public class ImpactedTestEngineConfiguration {

	/** The directory to write testwise coverage and available tests to. */
	final File reportDirectory;

	/** The test engine registry used to determine the {@link TestEngine}s to use. */
	final TestEngineRegistry testEngineRegistry;

	/** The {@link ITestExecutor} to use for execution of tests. */
	final ITestExecutor testExecutor;

	public ImpactedTestEngineConfiguration(
			File reportDirectory,
			TestEngineRegistry testEngineRegistry,
			ITestExecutor testExecutor) {
		this.reportDirectory = reportDirectory;
		this.testEngineRegistry = testEngineRegistry;
		this.testExecutor = testExecutor;
	}
}