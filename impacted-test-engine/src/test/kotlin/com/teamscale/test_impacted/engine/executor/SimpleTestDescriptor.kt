package com.teamscale.test_impacted.engine.executor

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor

/** A basic implementation of a [TestDescriptor] that can be used during tests.  */
class SimpleTestDescriptor private constructor(
	uniqueId: UniqueId,
	private val type: TestDescriptor.Type,
	displayName: String
) : AbstractTestDescriptor(uniqueId, displayName) {
	private var shouldSkip = false

	val dynamicTests = mutableListOf<TestDescriptor>()

	var executionResult: TestExecutionResult = TestExecutionResult.successful()
		private set

	override fun getType() = type

	/** Marks the test as being skipped.  */
	fun skip(): SimpleTestDescriptor {
		this.shouldSkip = true
		return this
	}

	/** Whether the test should be skipped.  */
	fun shouldBeSkipped() = shouldSkip

	/** Sets the execution result that the engine should report when simulating the test's execution.  */
	fun result(executionResult: TestExecutionResult): SimpleTestDescriptor {
		this.executionResult = executionResult
		return this
	}

	companion object {
		/** Creates a [TestDescriptor] for a concrete test case without children.  */
		fun testCase(uniqueId: UniqueId) =
			SimpleTestDescriptor(uniqueId, TestDescriptor.Type.TEST, getSimpleDisplayName(uniqueId))

		private fun getSimpleDisplayName(uniqueId: UniqueId) =
			uniqueId.segments[uniqueId.segments.size - 1].value

		/** Creates a [TestDescriptor] for a dynamic test case which registers children during test execution.  */
		fun dynamicTestCase(uniqueId: UniqueId, vararg dynamicTestCases: TestDescriptor): TestDescriptor {
			val simpleTestDescriptor = SimpleTestDescriptor(
				uniqueId, TestDescriptor.Type.CONTAINER_AND_TEST,
				getSimpleDisplayName(uniqueId)
			)
			simpleTestDescriptor.dynamicTests.addAll(listOf(*dynamicTestCases))
			return simpleTestDescriptor
		}

		/**
		 * Creates a [TestDescriptor] for a test container (e.g., a test class or test engine) containing other
		 * [TestDescriptor] children.
		 */
		fun testContainer(uniqueId: UniqueId, vararg children: TestDescriptor): SimpleTestDescriptor {
			val result = SimpleTestDescriptor(
				uniqueId, TestDescriptor.Type.CONTAINER,
				getSimpleDisplayName(uniqueId)
			)
			children.forEach { child ->
				result.addChild(child)
			}
			return result
		}
	}
}
