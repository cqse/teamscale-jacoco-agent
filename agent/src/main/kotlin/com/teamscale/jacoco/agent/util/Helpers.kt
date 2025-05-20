package com.teamscale.jacoco.agent.util

import com.teamscale.jacoco.agent.Main
import kotlin.time.measureTime

fun benchmark(name: String, action: () -> Unit) =
	measureTime { action() }.also { duration -> Main.logger.debug("$name took $duration") }