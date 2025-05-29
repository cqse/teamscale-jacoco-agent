package testframework

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.AgentHttpRequestFailedException
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import com.teamscale.tia.client.TiaAgent
import okhttp3.HttpUrl.Companion.toHttpUrl
import systemundertest.SystemUnderTest
import java.util.stream.Collectors

/** Simulates a custom test framework that doesn't rely on JUnit.  */
class CustomTestFramework(private val agentPort: Int) {
	private val allTests = hashMapOf<String, () -> Unit>()

	init {
		allTests.put("testFoo") {
			if (SystemUnderTest().foo() != 5) {
				throw AssertionError("Incorrect return value for foo")
			}
		}
		allTests.put("testBar") {
			if (SystemUnderTest().bar() != 7) {
				throw AssertionError("Incorrect return value for bar")
			}
		}
	}

	/** Talks to the TIA agent and runs all impacted tests. Also uploads a coverage report at the end.  */
	@Throws(AgentHttpRequestFailedException::class)
	fun runTestsWithTia() {
		val agent = TiaAgent(false, "http://localhost:$agentPort".toHttpUrl())
		val testRun = agent.startTestRun(
			allTests.keys
				.map { name -> ClusteredTestDetails(name, name, null, null, "test-partition") }
		)

		testRun.prioritizedClusters?.forEach { cluster ->
			cluster.tests?.forEach { test ->
				val runnable = allTests.get(test.testName)
				val runningTest = testRun.startTest(test.testName)
				try {
					runnable?.invoke()
					runningTest.endTest(TestResultWithMessage(ETestExecutionResult.PASSED, ""))
				} catch (t: Throwable) {
					runningTest.endTest(
						TestResultWithMessage(ETestExecutionResult.FAILURE, t.message)
					)
				}
			}
		}

		testRun.endTestRun(true)
	}
}
