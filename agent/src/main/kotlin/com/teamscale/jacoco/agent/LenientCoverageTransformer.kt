package com.teamscale.jacoco.agent

import org.jacoco.agent.rt.internal_0e20598.CoverageTransformer
import org.jacoco.agent.rt.internal_0e20598.IExceptionLogger
import org.jacoco.agent.rt.internal_0e20598.core.runtime.AgentOptions
import org.jacoco.agent.rt.internal_0e20598.core.runtime.IRuntime
import org.slf4j.Logger
import java.lang.instrument.IllegalClassFormatException
import java.security.ProtectionDomain

/**
 * A class file transformer which delegates to the JaCoCo [CoverageTransformer] to do the actual instrumentation,
 * but treats instrumentation errors e.g. due to unsupported class file versions more lenient by only logging them, but
 * not bailing out completely. Those unsupported classes will not be instrumented and will therefore not be contained in
 * the collected coverage report.
 */
// The coverage transformer only uses the logger to print an error when the instrumentation fails.
// We want to show our more specific error message instead, so we only log this for debugging at trace.
class LenientCoverageTransformer(
	runtime: IRuntime,
	options: AgentOptions,
	private val logger: Logger
) : CoverageTransformer(runtime, options, IExceptionLogger { e -> logger.trace(e.message, e) }) {
	override fun transform(
		loader: ClassLoader?,
		classname: String?,
		classBeingRedefined: Class<*>?,
		protectionDomain: ProtectionDomain?,
		classfileBuffer: ByteArray?
	): ByteArray? {
		try {
			return super.transform(loader, classname, classBeingRedefined, protectionDomain, classfileBuffer)
		} catch (e: IllegalClassFormatException) {
			logger.error(
				"Failed to instrument $classname. File will be skipped from instrumentation. " +
						"No coverage will be collected for it. Exclude the file from the instrumentation " +
						"or try updating the Teamscale JaCoCo Agent if the file should actually be instrumented. " +
						"(Cause: ${getRootCauseMessage(e) ?: "Root cause message unavailable"})"
			)
			return null
		}
	}

	private fun getRootCauseMessage(e: Throwable): String? =
		if (e.cause != null) {
			getRootCauseMessage(e.cause!!)
		} else {
			e.message
		}
}
