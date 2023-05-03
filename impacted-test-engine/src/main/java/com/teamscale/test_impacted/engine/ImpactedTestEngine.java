package com.teamscale.test_impacted.engine;

import com.teamscale.test_impacted.commons.LoggerUtils;
import com.teamscale.test_impacted.engine.options.TestEngineOptionUtils;
import com.teamscale.test_impacted.engine.options.TestEngineOptions;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.Optional;
import java.util.logging.Logger;

/** Test engine for executing impacted tests. */
public class ImpactedTestEngine implements TestEngine {

	/** The id of the {@link ImpactedTestEngine}. */
	public static final String ENGINE_ID = "teamscale-test-impacted";

	public static final Logger LOGGER = LoggerUtils.getLogger(ImpactedTestEngine.class);

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
				new InternalImpactedTestEngine(configuration, engineOptions.getPartition());

		return internalImpactedTestEngine.discover(discoveryRequest, uniqueId);
	}

	@Override
	public void execute(ExecutionRequest request) {
		// According to the TestEngine interface the request must correspond to the last execution request. Therefore, we
		// may re-use the configuration initialized during discovery.
		if (internalImpactedTestEngine == null) {
			throw new AssertionError("Can't execute request without discovering it first.");
		}
		internalImpactedTestEngine.execute(request);
	}

	@Override
	public Optional<String> getGroupId() {
		return Optional.of("com.teamscale");
	}

	@Override
	public Optional<String> getArtifactId() {
		return Optional.of("impacted-test-engine");
	}
}
