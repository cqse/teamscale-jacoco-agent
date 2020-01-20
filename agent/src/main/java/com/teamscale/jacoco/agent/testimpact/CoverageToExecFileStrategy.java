package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.model.TestExecution;
import org.slf4j.Logger;
import spark.Response;

import java.io.IOException;

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
	public void testEnd(String test, TestExecution testExecution,
						Response response) throws JacocoRuntimeController.DumpException {
		super.testEnd(test, testExecution, response);
		controller.dump();
		if (testExecution != null) {
			try {
				testExecutionWriter.append(testExecution);
			} catch (IOException e) {
				logger.error("Failed to store test execution: " + e.getMessage(), e);
			}
		}
		response.status(204);
	}
}
