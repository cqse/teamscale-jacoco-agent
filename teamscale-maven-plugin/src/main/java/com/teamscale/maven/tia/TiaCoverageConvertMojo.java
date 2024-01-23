package com.teamscale.maven.tia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.common.base.Strings;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.ClasspathUtils;
import com.teamscale.jacoco.agent.options.FilePatternResolver;

import shadow.com.teamscale.client.TestDetails;
import shadow.com.teamscale.report.EDuplicateClassFileBehavior;
import shadow.com.teamscale.report.ReportUtils;
import shadow.com.teamscale.report.testwise.ETestArtifactFormat;
import shadow.com.teamscale.report.testwise.TestwiseCoverageReportWriter;
import shadow.com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import shadow.com.teamscale.report.testwise.model.TestExecution;
import shadow.com.teamscale.report.testwise.model.factory.TestInfoFactory;
import shadow.com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import shadow.com.teamscale.report.util.CommandLineLogger;
import shadow.com.teamscale.report.util.ILogger;

/**
 * Batch converts all created .exec file reports into a testwise coverage
 * report.
 */
@Mojo(name = "testwise-coverage-converter", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TiaCoverageConvertMojo extends AbstractMojo {
	/**
	 * Wildcard include patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter(defaultValue = "**")
	public String[] includes;
	/**
	 * Wildcard exclude patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter()
	public String[] excludes;

	/**
	 * After how many tests the testwise coverage should be split into multiple
	 * reports (Default is 5000)."
	 */
	@Parameter(defaultValue = "5000")
	public int splitAfter;

	/**
	 * The project build directory (usually: {@code ./target}). Provided
	 * automatically by Maven.
	 */
	@Parameter(defaultValue = "${project.build.directory}")
	public String projectBuildDir;

	/**
	 * The output directory of the testwise coverage reports.
	 */
	@Parameter()
	public String outputFolder;

	private final ILogger logger = new CommandLineLogger();

	@Override
	public void execute() throws MojoFailureException {
		if (Strings.isNullOrEmpty(outputFolder)) {
			outputFolder = Paths.get(projectBuildDir, "tia", "reports").toString();
		}
		String reportFilePath = Paths.get(outputFolder, "testwise-coverage.json").toString();

		List<File> reportFiles = new ArrayList<>();
		reportFiles.add(Paths.get(projectBuildDir, "tia").toAbsolutePath().resolve("reports").toFile());

		logger.info("Generating the testwise coverage report");
		JaCoCoTestwiseReportGenerator generator = createJaCoCoTestwiseReportGenerator();
		TestInfoFactory testInfoFactory = createTestInfoFactory(reportFiles);
		List<File> jacocoExecutionDataList = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, reportFiles);
		try (TestwiseCoverageReportWriter coverageWriter = new TestwiseCoverageReportWriter(testInfoFactory,
				new File(reportFilePath), splitAfter)) {
			for (File executionDataFile : jacocoExecutionDataList) {
				logger.info("Writing execution data for file: " + executionDataFile.getName());
				generator.convertAndConsume(executionDataFile, coverageWriter);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private TestInfoFactory createTestInfoFactory(List<File> reportFiles) throws MojoFailureException {
		try {
			List<TestDetails> testDetails = ReportUtils.readObjects(ETestArtifactFormat.TEST_LIST, TestDetails[].class,
					reportFiles);
			List<TestExecution> testExecutions = ReportUtils.readObjects(ETestArtifactFormat.TEST_EXECUTION,
					TestExecution[].class, reportFiles);
			logger.info("Writing report with " + testDetails.size() + " Details/" + testExecutions.size() + " Results");
			return new TestInfoFactory(testDetails, testExecutions);
		} catch (IOException e) {
			logger.error("Could not read test details from reports. Aborting.");
			throw new MojoFailureException(e);
		}

	}

	private JaCoCoTestwiseReportGenerator createJaCoCoTestwiseReportGenerator() throws MojoFailureException {
		String includes = null;
		if (this.includes != null) {
			includes = String.join(":", this.includes);
		}
		String excludes = null;
		if (this.excludes != null) {
			excludes = String.join(":", this.excludes);
		}
		try {
			return new JaCoCoTestwiseReportGenerator(getClassDirectoriesOrZips(),
					new ClasspathWildcardIncludeFilter(includes, excludes), EDuplicateClassFileBehavior.WARN, logger);
		} catch (AgentOptionParseException e) {
			logger.error("Could not create testwise report generator. Aborting.");
			throw new MojoFailureException(e);
		}
	}

	private List<File> getClassDirectoriesOrZips() throws AgentOptionParseException {
		List<String> classDirectoriesOrZips = new ArrayList<>();
		classDirectoriesOrZips.add(projectBuildDir);
		return ClasspathUtils.resolveClasspathTextFiles("classes", new FilePatternResolver(new CommandLineLogger()),
				classDirectoriesOrZips);
	}
}