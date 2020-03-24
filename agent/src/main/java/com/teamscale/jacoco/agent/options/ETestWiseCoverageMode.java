package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.testimpact.TestEventHandlerStrategyBase;

/** Decides which {@link TestEventHandlerStrategyBase} is used in test-wise mode. */
public enum ETestWiseCoverageMode {
	/** Caches test-wise coverage in-memory and uploads a report to Teamscale. */
	TEAMSCALE_REPORT,
	/** Writes test-wise coverage to disk as .exec files. */
	EXEC_FILE,
	/** Returns test-wise coverage to the caller via HTTP. */
	HTTP
}
