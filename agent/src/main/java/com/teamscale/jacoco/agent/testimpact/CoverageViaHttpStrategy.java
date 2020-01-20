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
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.builder.TestInfoBuilder;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import spark.Response;

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
	public void testEnd(String test, TestExecution testExecution,
						Response response) throws JacocoRuntimeController.DumpException {
		super.testEnd(test, testExecution, response);
		Dump dump = controller.dumpAndReset();
		TestwiseCoverage testwiseCoverage = this.testwiseReportGenerator.convert(dump);
		TestInfoBuilder container = new TestInfoBuilder(test);
		container.setCoverage(CollectionUtils.getAny(testwiseCoverage.getTests()));
		if (testExecution != null) {
			container.setExecution(testExecution);
		}
		TestInfo testInfo = container.build();
		response.header("Content-Type", "application/json");
		response.body(testInfoJsonAdapter.toJson(testInfo));
		response.status(200);
	}
}
