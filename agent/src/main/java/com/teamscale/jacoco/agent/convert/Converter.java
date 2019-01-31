package com.teamscale.jacoco.agent.convert;

import com.teamscale.client.TestDetails;
import com.teamscale.jacoco.util.Benchmark;
import com.teamscale.jacoco.util.LoggingUtils;
import com.teamscale.report.ReportUtils;
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.ETestArtifactFormat;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import com.teamscale.report.util.AntPatternIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.teamscale.jacoco.util.LoggingUtils.wrap;

/** Converts one .exec binary coverage file to XML. */
public class Converter {

	/** The command line arguments. */
	private ConvertCommand arguments;

	/** Constructor. */
	public Converter(ConvertCommand arguments) {
		this.arguments = arguments;
	}

	/** Converts one .exec binary coverage file to XML. */
	public void runJaCoCoReportGeneration() throws IOException {
		List<File> jacocoExecutionDataList = ReportUtils.INSTANCE
				.listFiles(ETestArtifactFormat.JACOCO, arguments.getInputFiles());

		ExecFileLoader loader = new ExecFileLoader();
		for (File jacocoExecutionData : jacocoExecutionDataList) {
			loader.load(jacocoExecutionData);
		}

		SessionInfo sessionInfo = loader.getSessionInfoStore().getMerged("merged");
		ExecutionDataStore executionDataStore = loader.getExecutionDataStore();

		AntPatternIncludeFilter locationIncludeFilter = new AntPatternIncludeFilter(
				arguments.getLocationIncludeFilters(), arguments.getLocationExcludeFilters());
		Logger logger = LoggingUtils.getLogger(this);
		JaCoCoXmlReportGenerator generator = new JaCoCoXmlReportGenerator(arguments.getClassDirectoriesOrZips(),
				locationIncludeFilter, arguments.shouldIgnoreDuplicateClassFiles(), wrap(logger));

		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			String xml = generator.convert(new Dump(sessionInfo, executionDataStore));
			FileSystemUtils.writeFileUTF8(arguments.getOutputFile(), xml);
		}
	}

	/** Converts one .exec binary coverage file, test details and test execution files to JSON testwise coverage. */
	public void runTestwiseCoverageReportGeneration() throws IOException, CoverageGenerationException {
		List<TestDetails> testDetails = ReportUtils.INSTANCE.readObjects(ETestArtifactFormat.TEST_LIST,
				TestDetails[].class, arguments.getInputFiles());
		List<TestExecution> testExecutions = ReportUtils.INSTANCE.readObjects(ETestArtifactFormat.TEST_EXECUTION,
				TestExecution[].class, arguments.getInputFiles());

		List<File> jacocoExecutionDataList = ReportUtils.INSTANCE
				.listFiles(ETestArtifactFormat.JACOCO, arguments.getInputFiles());
		ILogger logger = new CommandLineLogger();
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(arguments.locationIncludeFilters,
				arguments.locationExcludeFilters);
		JaCoCoTestwiseReportGenerator generator = new JaCoCoTestwiseReportGenerator(
				arguments.getClassDirectoriesOrZips(),
				includeFilter,
				true,
				logger
		);

		try (Benchmark benchmark = new Benchmark("Generating the testwise coverage report")) {
			TestwiseCoverage coverage = generator.convert(jacocoExecutionDataList);
			logger.info(
					"Merging report with " + testDetails.size() + " Details/" + coverage.getTests()
							.size() + " Coverage/" + testExecutions.size() + " Results");

			TestwiseCoverageReport report = TestwiseCoverageReportBuilder.Companion
					.createFrom(testDetails, coverage.getTests(), testExecutions);
			ReportUtils.INSTANCE.writeReportToFile(arguments.getOutputFile(), report);
		}
	}
}
