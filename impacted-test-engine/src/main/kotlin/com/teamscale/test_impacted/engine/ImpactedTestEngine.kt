package com.teamscale.test_impacted.engine

import com.teamscale.test_impacted.commons.LoggerUtils.getLogger
import com.teamscale.test_impacted.engine.options.TestEngineOptionUtils
import org.junit.platform.engine.*
import java.util.*
import java.util.logging.Logger

/** Test engine for executing impacted tests.  */
class ImpactedTestEngine : TestEngine {
	private var internalImpactedTestEngine: InternalImpactedTestEngine? = null

	override fun getId() = ENGINE_ID

	override fun discover(
		discoveryRequest: EngineDiscoveryRequest,
		uniqueId: UniqueId
	): TestDescriptor {
		val engineOptions = TestEngineOptionUtils
			.getEngineOptions(discoveryRequest.configurationParameters)
		val configuration = engineOptions.createTestEngineConfiguration()
		val engine = InternalImpactedTestEngine(configuration, engineOptions.partition!!)

		// Re-initialize the configuration for this discovery (and optional following execution).
		internalImpactedTestEngine = engine

		return engine.discover(discoveryRequest, uniqueId)
	}

	override fun execute(request: ExecutionRequest) {
		// According to the TestEngine interface the request must correspond to the last execution request. Therefore, we
		// may re-use the configuration initialized during discovery.
		check(internalImpactedTestEngine != null) {
			"Can't execute request without discovering it first."
		}
		internalImpactedTestEngine?.execute(request)
	}

	override fun getGroupId() = Optional.of("com.teamscale")

	override fun getArtifactId() = Optional.of("impacted-test-engine")

	companion object {
		/** The id of the [ImpactedTestEngine].  */
		const val ENGINE_ID = "teamscale-test-impacted"

		@JvmField
		val LOGGER: Logger = getLogger(ImpactedTestEngine::class.java)
	}
}