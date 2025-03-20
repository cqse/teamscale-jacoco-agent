package testframework

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.tia.client.AgentHttpRequestFailedException
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import com.teamscale.tia.client.TiaAgent
import okhttp3.HttpUrl
import systemundertest.SystemUnderTest

/** Simulates a custom test framework that doesn't rely on JUnit.  */
class CustomTestFramework(private val agentPort: Int) {
	private val allTests = mutableMapOf<String, Runnable>()
	val testInfos = mutableListOf<TestInfo>()

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
		val testRun = agent.startTestRunWithoutTestSelection()

		allTests.keys.forEach { uniformPath ->
			val runnable = allTests[uniformPath]
			val runningTest = testRun.startTest(uniformPath)

			var testInfo: TestInfo
			try {
				runnable?.run()
				testInfo = runningTest.endTestAndRetrieveCoverage(
					TestResultWithMessage(ETestExecutionResult.PASSED, "")
				)
			} catch (t: Throwable) {
				testInfo = runningTest.endTestAndRetrieveCoverage(
					TestResultWithMessage(ETestExecutionResult.FAILURE, t.message)
				)
			}
			testInfos.add(testInfo)
		}
	}
}
