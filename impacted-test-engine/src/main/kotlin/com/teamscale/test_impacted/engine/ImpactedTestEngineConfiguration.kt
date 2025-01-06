package com.teamscale.test_impacted.engine

import com.teamscale.test_impacted.engine.executor.ITestSorter
import com.teamscale.test_impacted.engine.executor.TeamscaleAgentNotifier

/**
 * Configuration class for the Impacted Test Engine, responsible for managing dependencies and behavior
 * required for executing impacted tests. It provides access to utilities for test data management,
 * test engine discovery, test sorting, and interaction with external agents.
 *
 * @property testDataWriter Used to handle the writing of test execution data and test details to files.
 * @property testEngineRegistry Manages and provides access to available test engines, filtering and iterating over them.
 * @property testSorter Defines the logic for selecting and sorting tests for execution.
 * @property teamscaleAgentNotifier Facilitates communication with the Teamscale test-wise coverage agent by signaling test lifecycle events.
 */
class ImpactedTestEngineConfiguration(
	val testDataWriter: TestDataWriter,
	val testEngineRegistry: TestEngineRegistry,
	val testSorter: ITestSorter,
	val teamscaleAgentNotifier: TeamscaleAgentNotifier
)