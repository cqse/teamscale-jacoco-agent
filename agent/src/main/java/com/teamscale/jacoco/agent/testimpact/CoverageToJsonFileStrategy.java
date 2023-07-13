
package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

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
