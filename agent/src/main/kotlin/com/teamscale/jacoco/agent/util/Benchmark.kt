package com.teamscale.jacoco.agent.util

import com.teamscale.jacoco.agent.util.Logging.logger
import kotlin.time.measureTime

object Benchmark {
	/**
	 * Measures how long a certain piece of code takes and logs it to the debug log.
	 */
	fun Any.benchmark(name: String, block: () -> Unit) =
		logger.debug("$name took ${measureTime(block)} ms")
}