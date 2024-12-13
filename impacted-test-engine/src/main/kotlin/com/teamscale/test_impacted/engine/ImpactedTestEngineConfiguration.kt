package com.teamscale.test_impacted.engine

import com.teamscale.test_impacted.engine.executor.ITestSorter
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier

/** Container for a configuration used by the [ImpactedTestEngine]  */
class ImpactedTestEngineConfiguration(
	/** The directory to write testwise coverage and available tests to.  */
	val testDataWriter: TestDataWriter,
	/** The test engine registry used to determine the [TestEngine]s to use.  */
	val testEngineRegistry: TestEngineRegistry,
	/** The [ITestSorter] to use for execution of tests.  */
	val testSorter: ITestSorter,
	/** An API to signal test start and end to the agent.  */
	val teamscaleAgentNotifier: TeamscaleAgentNotifier
)