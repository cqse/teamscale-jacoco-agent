package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.tia.client.ITestwiseCoverageAgentApi;
import com.teamscale.tia.client.UrlUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.io.IOException;
import java.util.List;

/** Communicates test start and end to the agent and the end of the overall test execution. */
public class TeamscaleAgentNotifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestwiseCoverageCollectingTestExecutor.class);

	/** A list of API services to signal test start and end to the agent. */
	private final List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis;

	/**
	 * Whether only a part of the tests is being executed (<code>true</code>) or whether all tests are executed
	 * (<code>false</code>).
	 */
	private final boolean partial;

	public TeamscaleAgentNotifier(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis, boolean partial) {
		this.testwiseCoverageAgentApis = testwiseCoverageAgentApis;
		this.partial = partial;
	}

	/** Reports the start of a test to the Teamscale JaCoCo agent. */
	public void startTest(String testUniformPath) {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testStarted(UrlUtils.percentEncode(testUniformPath)).execute();
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error while calling service api.");
		}
	}

	/** Reports the end of a test to the Teamscale JaCoCo agent. */
	public void endTest(String testUniformPath, TestExecution testExecution) {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				if (testExecution == null) {
					apiService.testFinished(UrlUtils.percentEncode(testUniformPath)).execute();
				} else {
					apiService.testFinished(UrlUtils.percentEncode(testUniformPath), testExecution).execute();
				}
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error contacting test wise coverage agent.");
		}
	}

	/** Reports the end of the test run to the Teamscale JaCoCo agent. */
	public void testRunEnded() {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testRunFinished(partial).execute();
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error contacting test wise coverage agent.");
		}
	}
}
