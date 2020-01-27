package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.model.TestExecution;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Strategy for appending coverage into one exec file with one session per test. Execution data will be stored in a json
 * file side-by-side with the exec file. Test executions are also appended into a single file.
 */
public class CoverageToExecFileStrategy extends TestEventHandlerStrategyBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	/** Helper for writing test executions to disk. */
	private final TestExecutionWriter testExecutionWriter;

	public CoverageToExecFileStrategy(TestExecutionWriter testExecutionWriter,
									  JacocoRuntimeController controller) {
		super(controller);
		this.testExecutionWriter = testExecutionWriter;
	}

	@Override
	public void testStart(String test) {
		super.testStart(test);
	}

	@Override
	public String testEnd(String test, TestExecution testExecution) throws JacocoRuntimeController.DumpException {
		super.testEnd(test, testExecution);
		controller.dump();
		if (testExecution != null) {
			try {
				testExecutionWriter.append(testExecution);
			} catch (IOException e) {
				logger.error("Failed to store test execution: " + e.getMessage(), e);
			}
		}
		return null;
	}
}
