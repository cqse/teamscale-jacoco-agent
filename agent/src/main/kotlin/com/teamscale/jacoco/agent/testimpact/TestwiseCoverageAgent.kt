/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact

import com.teamscale.jacoco.agent.AgentBase
import com.teamscale.jacoco.agent.GenericExceptionMapper
import com.teamscale.jacoco.agent.logging.LoggingUtils
import com.teamscale.jacoco.agent.options.AgentOptions
import com.teamscale.jacoco.agent.options.ETestwiseCoverageMode
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.ServerProperties
import java.io.IOException
import java.lang.Boolean
import kotlin.Throws

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
class TestwiseCoverageAgent(
	options: AgentOptions,
	testExecutionWriter: TestExecutionWriter?,
	reportGenerator: JaCoCoTestwiseReportGenerator
) : AgentBase(options) {
	/**
	 * The test event strategy handler.
	 */
	@JvmField
	val testEventHandler = when (options.getTestwiseCoverageMode()) {
		ETestwiseCoverageMode.TEAMSCALE_UPLOAD -> CoverageToTeamscaleStrategy(controller, options, reportGenerator)
		ETestwiseCoverageMode.DISK -> CoverageToDiskStrategy(controller, options, reportGenerator)
		ETestwiseCoverageMode.HTTP -> CoverageViaHttpStrategy(controller, options, reportGenerator)
		else -> CoverageToExecFileStrategy(controller, options, testExecutionWriter)
	}

	init {
		// Set to empty to not end up with a default session in case no tests are executed,
		// which in turn causes a warning because we didn't write a corresponding test detail
		controller.sessionId = ""
	}

	override fun initResourceConfig(): ResourceConfig? {
		val resourceConfig = ResourceConfig()
		resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE.toString())
		TestwiseCoverageResource.setAgent(this)
		return resourceConfig.register(TestwiseCoverageResource::class.java)
			.register(GenericExceptionMapper::class.java)
	}

	override fun dumpReport() {
		// Dumping via the API is not supported in testwise mode. Ending the test run dumps automatically
	}

	companion object {
		/** Creates a [TestwiseCoverageAgent] based on the given options.  */
		@JvmStatic
		@Throws(IOException::class)
		fun create(agentOptions: AgentOptions): TestwiseCoverageAgent {
			val logger = LoggingUtils.getLogger(JaCoCoTestwiseReportGenerator::class.java)
			val reportGenerator = JaCoCoTestwiseReportGenerator(
				agentOptions.getClassDirectoriesOrZips(),
				agentOptions.locationIncludeFilter,
				agentOptions.getDuplicateClassFileBehavior(),
				LoggingUtils.wrap(logger)
			)
			return TestwiseCoverageAgent(
				agentOptions,
				TestExecutionWriter(agentOptions.createNewFileInOutputDirectory("test-execution", "json")),
				reportGenerator
			)
		}
	}
}
