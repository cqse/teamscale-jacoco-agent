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
 * Strategy which directly converts the collected coverage into a json object in place and returns the result to the
 * caller as response to the http request. If a test execution is given it is merged into the representation and
 * returned together called together with the coverage
 */
public class CoverageViaHttpStrategy extends TestEventHandlerStrategyBase {

	/** The logger. */
	protected final Logger logger = LoggingUtils.getLogger(this);

	private final JaCoCoTestwiseReportGenerator testwiseReportGenerator;

	private JsonAdapter<TestInfo> testInfoJsonAdapter = new Moshi.Builder().build().adapter(TestInfo.class)
			.indent("\t");

	public CoverageViaHttpStrategy(AgentOptions options,
								   JacocoRuntimeController controller) throws CoverageGenerationException {
		super(controller);
		testwiseReportGenerator = new JaCoCoTestwiseReportGenerator(
				options.getClassDirectoriesOrZips(),
				options.getLocationIncludeFilter(),
				options.getDuplicateClassFileBehavior(),
				LoggingUtils.wrap(logger));
	}

	@Override
	public void testStart(String test) {
		super.testStart(test);
	}

	@Override
	public String testEnd(String test, TestExecution testExecution) throws JacocoRuntimeController.DumpException {
		super.testEnd(test, testExecution);
		TestInfoBuilder builder = new TestInfoBuilder(test);
		Dump dump = controller.dumpAndReset();
		builder.setCoverage(this.testwiseReportGenerator.convert(dump));
		if (testExecution != null) {
			builder.setExecution(testExecution);
		}
		TestInfo testInfo = builder.build();
		return testInfoJsonAdapter.toJson(testInfo);
	}
}
