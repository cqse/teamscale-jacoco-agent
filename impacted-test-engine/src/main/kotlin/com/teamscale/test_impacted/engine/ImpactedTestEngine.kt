package com.teamscale.test_impacted.engine

import com.teamscale.test_impacted.commons.LoggerUtils.createLogger
import com.teamscale.test_impacted.engine.options.TestEngineOptionUtils
import org.junit.platform.engine.*
import java.util.*

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
		val configuration = engineOptions.testEngineConfiguration
		val engine = InternalImpactedTestEngine(configuration, engineOptions.partition!!)

		// Re-initialize the configuration for this discovery (and optional following execution).
		internalImpactedTestEngine = engine

		return engine.discover(discoveryRequest, uniqueId)
	}

	override fun execute(request: ExecutionRequest) {
		// According to the TestEngine interface the request must correspond to the last execution request. Therefore, we
		// may re-use the configuration initialized during discovery.
		requireNotNull(internalImpactedTestEngine) {
			"Can't execute request without discovering it first."
		}
		internalImpactedTestEngine?.execute(request)
	}

	override fun getGroupId() = Optional.of("com.teamscale")

	override fun getArtifactId() = Optional.of("impacted-test-engine")

	companion object {
		/** The id of the [ImpactedTestEngine].  */
		const val ENGINE_ID = "teamscale-test-impacted"

		val LOG = createLogger()
	}
}