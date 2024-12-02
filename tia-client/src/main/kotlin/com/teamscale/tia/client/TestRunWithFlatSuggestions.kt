package com.teamscale.tia.client

import com.teamscale.client.PrioritizableTest

/**
 * Represents a run of a flat list of prioritized and selected tests as reported by the TIA. Use this class to report
 * test start and end events and upload testwise coverage to Teamscale.
 *
 *
 * The caller of this class should retrieve the tests to execute from [prioritizedTests], run them (in the
 * given order if possible) and report test start and end events via [startTest])}.
 *
 *
 * After having run all tests, call [endTestRun] to create a testwise coverage report and upload it to
 * Teamscale.
 */
class TestRunWithFlatSuggestions internal constructor(
	api: ITestwiseCoverageAgentApi,
	private val prioritizedTests: List<PrioritizableTest>
) : TestRun(api)
