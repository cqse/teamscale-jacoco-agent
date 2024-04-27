package com.teamscale.jacoco.agent.util

import org.slf4j.Logger

object Logging {
	val Any.logger: Logger get () = LoggingUtils.getLogger(this)
}