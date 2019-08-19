package com.teamscale.test_impacted.engine;

import com.teamscale.test_impacted.engine.options.TestEngineOptionUtils;
import com.teamscale.test_impacted.engine.options.TestEngineOptions;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

/** Test engine for executing impacted tests. */
public class ImpactedTestEngine implements TestEngine {

	/** The id of the {@link ImpactedTestEngine}. */
	static final String ENGINE_ID = "teamscale-test-impacted";

	private InternalImpactedTestEngine internalImpactedTestEngine = null;

	@Override
	public String getId() {
		return ENGINE_ID;
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		TestEngineOptions engineOptions = TestEngineOptionUtils
				.getEngineOptions(discoveryRequest.getConfigurationParameters());
		ImpactedTestEngineConfiguration configuration = engineOptions.createTestEngineConfiguration();

		// Re-initialize the configuration for this discovery (and optional following execution).
		internalImpactedTestEngine =
				new InternalImpactedTestEngine(configuration.testEngineRegistry, configuration.testExecutor,
						new TestDataWriter(configuration.reportDirectory));

		return internalImpactedTestEngine.discover(discoveryRequest, uniqueId);
	}

	@Override
	public void execute(ExecutionRequest request) {
		// According to the TestEngine interface the request must correspond to the last execution request. Therefore we
		// may re-use the configuration initialized during discovery.
		if (internalImpactedTestEngine == null) {
			throw new AssertionError("Can't execute request without discovering it first.");
		}
		internalImpactedTestEngine.execute(request);
	}
}
