
package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.IOException;

/**
 * Strategy for appending coverage into one json test-wise coverage file with one session per test.
 */
public class CoverageToJsonFileStrategy extends CoverageToJsonStrategyBase {

	public CoverageToJsonFileStrategy(JacocoRuntimeController controller, AgentOptions agentOptions,
									  JaCoCoTestwiseReportGenerator reportGenerator) {
		super(controller, agentOptions, reportGenerator);
	}

	@Override
	protected void handleTestwiseCoverageJsonReady(String json) throws IOException {
		File reportFile = agentOptions.createNewFileInOutputDirectory("testwise-coverage", "json");
		FileSystemUtils.writeFileUTF8(reportFile, json);
	}
}
