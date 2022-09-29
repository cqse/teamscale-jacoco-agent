package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.builder.TestInfoBuilder;
import org.slf4j.Logger;

/**
 * Strategy which directly converts the collected coverage into a JSON object in place and returns the result to the
 * caller as response to the http request. If a test execution is given it is merged into the representation and
 * returned together with the coverage.
 */
public class CoverageViaHttpStrategy extends TestEventHandlerStrategyBase {

	private final Logger logger = LoggingUtils.getLogger(this);

	private final JsonAdapter<TestInfo> testInfoJsonAdapter = new Moshi.Builder().build().adapter(TestInfo.class)
			.indent("\t");
	private final JaCoCoTestwiseReportGenerator reportGenerator;

	public CoverageViaHttpStrategy(JacocoRuntimeController controller, AgentOptions agentOptions,
								   JaCoCoTestwiseReportGenerator reportGenerator) {
		super(agentOptions, controller);
		this.reportGenerator = reportGenerator;
	}

	@Override
	public String testEnd(String test, TestExecution testExecution)
			throws JacocoRuntimeController.DumpException, CoverageGenerationException {
		super.testEnd(test, testExecution);

		TestInfoBuilder builder = new TestInfoBuilder(test);
		Dump dump = controller.dumpAndReset();
		reportGenerator.updateClassDirCache();
		builder.setCoverage(reportGenerator.convert(dump));
		if (testExecution != null) {
			builder.setExecution(testExecution);
		}
		TestInfo testInfo = builder.build();
		String testInfoJson = testInfoJsonAdapter.toJson(testInfo);
		logger.debug("Generated test info {}", testInfoJson);
		return testInfoJson;
	}
}
