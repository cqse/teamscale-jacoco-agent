package com.teamscale.jacoco.agent.convert;

import com.teamscale.client.TestDetails;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.ReportUtils;
import com.teamscale.report.jacoco.EmptyReportException;
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.ETestArtifactFormat;
import com.teamscale.report.testwise.TestwiseCoverageReportWriter;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.factory.TestInfoFactory;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.CommandLineLogger;
import com.teamscale.report.util.ILogger;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.teamscale.jacoco.agent.util.LoggingUtils.wrap;

/** Converts one .exec binary coverage file to XML. */
public class Converter {

	/** The command line arguments. */
	private ConvertCommand arguments;

	/** Constructor. */
	public Converter(ConvertCommand arguments) {
		this.arguments = arguments;
	}

	/** Converts one .exec binary coverage file to XML. */
	public void runJaCoCoReportGeneration() throws IOException, AgentOptionParseException {
		List<File> jacocoExecutionDataList = ReportUtils
				.listFiles(ETestArtifactFormat.JACOCO, arguments.getInputFiles());

		ExecFileLoader loader = new ExecFileLoader();
		for (File jacocoExecutionData : jacocoExecutionDataList) {
			loader.load(jacocoExecutionData);
		}

		SessionInfo sessionInfo = loader.getSessionInfoStore().getMerged("merged");
		ExecutionDataStore executionDataStore = loader.getExecutionDataStore();

		Logger logger = LoggingUtils.getLogger(this);
		JaCoCoXmlReportGenerator generator = new JaCoCoXmlReportGenerator(arguments.getClassDirectoriesOrZips(),
				getWildcardIncludeExcludeFilter(), arguments.getDuplicateClassFileBehavior(), arguments.shouldIgnoreUncoveredClasses,
				wrap(logger));

		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			generator.convert(new Dump(sessionInfo, executionDataStore), Paths.get(arguments.outputFile).toFile());
		} catch (EmptyReportException e) {
			logger.warn("Converted report was emtpy.", e);
		}
	}

	/** Converts one .exec binary coverage file, test details and test execution files to JSON testwise coverage. */
	public void runTestwiseCoverageReportGeneration() throws IOException, CoverageGenerationException, AgentOptionParseException {
		List<TestDetails> testDetails = ReportUtils.readObjects(ETestArtifactFormat.TEST_LIST,
				TestDetails[].class, arguments.getInputFiles());
		List<TestExecution> testExecutions = ReportUtils.readObjects(ETestArtifactFormat.TEST_EXECUTION,
				TestExecution[].class, arguments.getInputFiles());

		List<File> jacocoExecutionDataList = ReportUtils
				.listFiles(ETestArtifactFormat.JACOCO, arguments.getInputFiles());
		ILogger logger = new CommandLineLogger();

		JaCoCoTestwiseReportGenerator generator = new JaCoCoTestwiseReportGenerator(
				arguments.getClassDirectoriesOrZips(),
				getWildcardIncludeExcludeFilter(),
				arguments.getDuplicateClassFileBehavior(),
				logger
		);

		TestInfoFactory testInfoFactory = new TestInfoFactory(testDetails, testExecutions);

		try (Benchmark benchmark = new Benchmark("Generating the testwise coverage report")) {
			logger.info(
					"Writing report with " + testDetails.size() + " Details/" + testExecutions.size() + " Results");

			try (TestwiseCoverageReportWriter coverageWriter = new TestwiseCoverageReportWriter(testInfoFactory,
					arguments.getOutputFile(), arguments.getSplitAfter())) {
				for (File executionDataFile : jacocoExecutionDataList) {
					generator.convertAndConsume(executionDataFile, coverageWriter);
				}
			}
		}
	}

	private ClasspathWildcardIncludeFilter getWildcardIncludeExcludeFilter() {
		return new ClasspathWildcardIncludeFilter(
				String.join(":", arguments.getLocationIncludeFilters()),
				String.join(":", arguments.getLocationExcludeFilters()));
	}

}
