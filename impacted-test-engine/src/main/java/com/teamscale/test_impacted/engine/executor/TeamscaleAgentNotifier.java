package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.commons.LoggerUtils;
import com.teamscale.tia.client.ITestwiseCoverageAgentApi;
import com.teamscale.tia.client.UrlUtils;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Communicates test start and end to the agent and the end of the overall test execution. */
public class TeamscaleAgentNotifier {

	private static final Logger LOGGER = LoggerUtils.getLogger(TeamscaleAgentNotifier.class);

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
				apiService.testStarted(UrlUtils.encodeUrl(testUniformPath)).execute();
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Error while calling service api.");
		}
	}

	/** Reports the end of a test to the Teamscale JaCoCo agent. */
	public void endTest(String testUniformPath, TestExecution testExecution) {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				if (testExecution == null) {
					apiService.testFinished(UrlUtils.encodeUrl(testUniformPath)).execute();
				} else {
					apiService.testFinished(UrlUtils.encodeUrl(testUniformPath), testExecution).execute();
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Error contacting test wise coverage agent.");
		}
	}

	/** Reports the end of the test run to the Teamscale JaCoCo agent. */
	public void testRunEnded() {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testRunFinished(partial).execute();
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Error contacting test wise coverage agent.");
		}
	}
}
