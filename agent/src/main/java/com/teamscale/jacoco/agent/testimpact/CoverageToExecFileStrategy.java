package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.logging.LoggingUtils;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Strategy for appending coverage into one exec file with one session per test. Execution data will be stored in a json
 * file side-by-side with the exec file. Test executions are also appended into a single file.
 */
public class CoverageToExecFileStrategy extends TestEventHandlerStrategyBase {

	private final Logger logger = LoggingUtils.getLogger(this);

	/** Helper for writing test executions to disk. */
	private final TestExecutionWriter testExecutionWriter;

	public CoverageToExecFileStrategy(JacocoRuntimeController controller, AgentOptions agentOptions,
									  TestExecutionWriter testExecutionWriter) {
		super(agentOptions, controller);
		this.testExecutionWriter = testExecutionWriter;
	}

	@Override
	public TestInfo testEnd(String test,
							TestExecution testExecution) throws JacocoRuntimeController.DumpException, CoverageGenerationException {
		logger.debug("Test {} ended with execution {}. Writing exec file and test execution", test, testExecution);
		super.testEnd(test, testExecution);
		controller.dump();
		// Ensures that the coverage collected between the last test and the JVM shutdown
		// is not considered a test with the same name as the last test
		controller.setSessionId("");
		if (testExecution != null) {
			try {
				testExecutionWriter.append(testExecution);
				logger.debug("Successfully wrote test execution for {}", test);
			} catch (IOException e) {
				logger.error("Failed to store test execution: {}", e.getMessage(), e);
			}
		}
		return null;
	}
}
