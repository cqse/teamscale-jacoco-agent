package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.report.testwise.model.TestExecution;
import spark.Response;

public abstract class TestEventHandlerStrategyBase {

	protected final JacocoRuntimeController controller;

	/** The timestamp at which the /test/start endpoint has been called last time. */
	private long startTimestamp;

	public TestEventHandlerStrategyBase(JacocoRuntimeController controller) {
		this.controller = controller;
	}

	public void testStart(String test) {
		// Dump and reset coverage so that we only record coverage that belongs to this particular test case.
		controller.reset();
		controller.setSessionId(test);
		startTimestamp = System.currentTimeMillis();
	}

	public void testEnd(String test, TestExecution testExecution,
						Response response) throws JacocoRuntimeController.DumpException {
		if (testExecution != null) {
			long endTimestamp = System.currentTimeMillis();
			testExecution.setDurationMillis(endTimestamp - startTimestamp);
		}
	}
}
