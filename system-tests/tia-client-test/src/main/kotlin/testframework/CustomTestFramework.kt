package testframework

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.AgentHttpRequestFailedException
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import com.teamscale.tia.client.TiaAgent
import okhttp3.HttpUrl
import systemundertest.SystemUnderTest

/** Simulates a custom test framework that doesn't rely on JUnit.  */
class CustomTestFramework(private val agentPort: Int) {
	private val allTests = mutableMapOf<String, Runnable>()

	init {
		allTests["testFoo"] = Runnable {
			if (SystemUnderTest().foo() != 5) {
				throw AssertionError("Incorrect return value for foo")
			}
		}
		allTests["testBar"] = Runnable {
			if (SystemUnderTest().bar() != 7) {
				throw AssertionError("Incorrect return value for bar")
			}
		}
	}

	/** Talks to the TIA agent and runs all impacted tests. Also uploads a coverage report at the end.  */
	@Throws(AgentHttpRequestFailedException::class)
	fun runTestsWithTia() {
		val agent = TiaAgent(false, HttpUrl.get("http://localhost:$agentPort"))
		val testRun = agent.startTestRun(
			allTests.keys.map { name ->
				ClusteredTestDetails(name, name, null, null, null)
			}
		)

		testRun.prioritizedClusters?.forEach { cluster ->
			cluster.tests?.forEach { (testName) ->
				val runnable = allTests[testName]
				val runningTest = testRun.startTest(testName)
				try {
					runnable?.run()
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
