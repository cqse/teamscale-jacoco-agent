package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.IOException;
import java.util.List;

/** Base class for strategies to handle test start and end events. */
public abstract class TestEventHandlerStrategyBase {

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	/** The timestamp at which the /test/start endpoint has been called last time. */
	private long startTimestamp;

	protected TestEventHandlerStrategyBase(JacocoRuntimeController controller) {
		this.controller = controller;
	}

	/** Called when test test with the given name is about to start. */
	public void testStart(String test) {
		// Dump and reset coverage so that we only record coverage that belongs to this particular test case.
		controller.reset();
		controller.setSessionId(test);
		startTimestamp = System.currentTimeMillis();
	}

	/**
	 * Called when the test with the given name finished.
	 *
	 * @param test          Uniform path of the test
	 * @param testExecution A test execution object holding the test result and error message. May be null if non is
	 *                      given in the request.
	 * @return The body of the response. <code>null</code> indicates "204 No content". Non-null results will be treated
	 * as json response.
	 */
	public String testEnd(String test, TestExecution testExecution) throws JacocoRuntimeController.DumpException {
		if (testExecution != null) {
			long endTimestamp = System.currentTimeMillis();
			testExecution.setDurationMillis(endTimestamp - startTimestamp);
		}
		return null;
	}

	public String testRunStart(List<ClusteredTestDetails> availableTests, boolean includeNonImpactedTests,
							   long baseline) throws IOException {
		return null;
	}

	public void testRunEnd() throws IOException {
		// base implementation does nothing
	}
}
