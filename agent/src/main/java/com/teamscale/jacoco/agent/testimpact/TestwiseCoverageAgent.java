/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/**
	 * The test event strategy handler.
	 */
	protected final TestEventHandlerStrategyBase testEventHandler;


	public TestwiseCoverageAgent(AgentOptions options, TestExecutionWriter testExecutionWriter,
								 JaCoCoTestwiseReportGenerator reportGenerator) throws IllegalStateException {
		super(options);
		switch (options.getTestwiseCoverageMode()) {
			case TEAMSCALE_UPLOAD:
				testEventHandler = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);
				break;
			case HTTP:
				testEventHandler = new CoverageViaHttpStrategy(controller, options, reportGenerator);
				break;
			default:
				testEventHandler = new CoverageToExecFileStrategy(controller, options, testExecutionWriter);
				break;
		}

	}

	@Override
	protected ResourceConfig initResourceConfig() {
		ResourceConfig resourceConfig = new ResourceConfig();
		TestwiseCoverageResource.setAgent(this);
		return resourceConfig.register(TestwiseCoverageResource.class);
	}
}
