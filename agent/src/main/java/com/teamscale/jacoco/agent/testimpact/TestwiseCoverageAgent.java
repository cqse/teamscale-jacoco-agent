/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.testimpact;

import java.io.IOException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;

import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens
 * for test events.
 */
public class TestwiseCoverageAgent extends AgentBase {

	/**
	 * The test event strategy handler.
	 */
	protected final TestEventHandlerStrategyBase testEventHandler;

	/** Creates a {@link TestwiseCoverageAgent} based on the given options. */
	public static TestwiseCoverageAgent create(AgentOptions agentOptions) throws IOException, UploaderException {
		Logger logger = LoggingUtils.getLogger(JaCoCoTestwiseReportGenerator.class);
		JaCoCoTestwiseReportGenerator reportGenerator = new JaCoCoTestwiseReportGenerator(
				agentOptions.getClassDirectoriesOrZips(), agentOptions.getLocationIncludeFilter(),
				agentOptions.getDuplicateClassFileBehavior(), LoggingUtils.wrap(logger));
		return new TestwiseCoverageAgent(agentOptions,
				new TestExecutionWriter(agentOptions.createNewFileInOutputDirectory("test-execution", "json")),
				reportGenerator);
	}

	public TestwiseCoverageAgent(AgentOptions options, TestExecutionWriter testExecutionWriter,
			JaCoCoTestwiseReportGenerator reportGenerator) throws IllegalStateException, UploaderException {
		super(options);
		switch (options.getTestwiseCoverageMode()) {
		case TEAMSCALE_UPLOAD:
			testEventHandler = new CoverageToTeamscaleStrategy(controller, options, reportGenerator);
			break;
		case DISK:
			testEventHandler = new CoverageToDiskStrategy(controller, options, reportGenerator);
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
		resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE.toString());
		TestwiseCoverageResource.setAgent(this);
		return resourceConfig.register(TestwiseCoverageResource.class);
	}
}
