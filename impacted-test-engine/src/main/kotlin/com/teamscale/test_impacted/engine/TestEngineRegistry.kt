package com.teamscale.test_impacted.engine

import org.junit.platform.commons.util.ClassLoaderUtils
import org.junit.platform.engine.TestEngine
import java.util.*

/**
 * A registry for managing and accessing available [TestEngine] implementations.
 * This class utilizes a filtering mechanism based on included and excluded test engine IDs
 * to determine the available engines in the current environment.
 *
 * @param includedTestEngineIds Set of test engine IDs to explicitly include.
 * @param excludedTestEngineIds Set of test engine IDs to explicitly exclude.
 */
open class TestEngineRegistry(
	includedTestEngineIds: Set<String>,
	excludedTestEngineIds: Set<String>
) : Iterable<TestEngine> {
	private val testEnginesById: Map<String, TestEngine>

	init {
		var otherTestEngines = loadOtherTestEngines(excludedTestEngineIds)

		// If there are no test engines set we don't need to filter but simply use all other test engines.
		if (includedTestEngineIds.isNotEmpty()) {
			otherTestEngines = otherTestEngines.filter { testEngine ->
				includedTestEngineIds.contains(testEngine.id)
			}
		}

		testEnginesById = otherTestEngines.associateBy { it.id }
	}

	/**
	 * Uses the [ServiceLoader] to discover all [TestEngine]s but the [ImpactedTestEngine] and the
	 * excluded test engines.
	 */
	private fun loadOtherTestEngines(excludedTestEngineIds: Set<String>) =
		ServiceLoader.load(
			TestEngine::class.java, ClassLoaderUtils.getDefaultClassLoader()
		).filter {
			ImpactedTestEngine.ENGINE_ID != it.id && !excludedTestEngineIds.contains(it.id)
		}

	/** Returns the [TestEngine] for the engine id or null if none is present.  */
	fun getTestEngine(engineId: String) = testEnginesById[engineId]

	override fun iterator() =
		testEnginesById.values.sortedBy { it.id }.iterator()
}
