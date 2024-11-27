package com.teamscale.tia.client

import com.teamscale.client.PrioritizableTestCluster

/**
 * Represents a run of prioritized and selected test clusters as reported by the TIA. Use this class to report test
 * start and end events and upload testwise coverage to Teamscale.
 *
 *
 * The caller of this class should retrieve the test clusters to execute from [.getPrioritizedClusters], run
 * them (in the given order if possible) and report test start and end events via [.startTest] )}.
 *
 *
 * After having run all tests, call [.endTestRun] to create a testwise coverage report and upload it to
 * Teamscale.
 */
class TestRunWithClusteredSuggestions internal constructor(
	api: ITestwiseCoverageAgentApi,
	@JvmField val prioritizedClusters: List<PrioritizableTestCluster>?
) : TestRun(api)
