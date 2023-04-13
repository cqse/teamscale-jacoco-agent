package com.teamscale.test_impacted.engine;

import com.teamscale.test_impacted.engine.options.TestEngineOptionUtils;
import com.teamscale.test_impacted.engine.options.TestEngineOptions;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Test engine for executing impacted tests. */
public class ImpactedTestEngine implements TestEngine {

	/** The id of the {@link ImpactedTestEngine}. */
	public static final String ENGINE_ID = "teamscale-test-impacted";

	public static Logger LOGGER;

	static {
		Logger mainLogger = Logger.getLogger("com.teamscale");
		mainLogger.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {
			private static final String format = "[%1$s] %2$s%n";

			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format,
						lr.getLevel().getLocalizedName(),
						lr.getMessage()
				);
			}
		});
		mainLogger.addHandler(handler);
		LOGGER = Logger.getLogger(ImpactedTestEngine.class.getName());
	}

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
		// According to the TestEngine interface the request must correspond to the last execution request. Therefore we
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
