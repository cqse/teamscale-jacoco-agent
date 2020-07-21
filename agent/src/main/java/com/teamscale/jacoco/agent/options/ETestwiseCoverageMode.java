package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.testimpact.TestEventHandlerStrategyBase;

/** Decides which {@link TestEventHandlerStrategyBase} is used in testwise mode. */
public enum ETestwiseCoverageMode {
	/** Caches testwise coverage in-memory and uploads a report to Teamscale. */
	TEAMSCALE_UPLOAD,
	/** Writes testwise coverage to disk as .exec files. */
	EXEC_FILE,
	/** Returns testwise coverage to the caller via HTTP. */
	HTTP
}
