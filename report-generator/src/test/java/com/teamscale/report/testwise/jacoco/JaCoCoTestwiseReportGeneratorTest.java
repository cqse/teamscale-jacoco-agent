package com.teamscale.report.testwise.jacoco;

import com.teamscale.client.TestDetails;
import com.teamscale.report.ReportUtils;
import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import com.teamscale.report.util.AntPatternIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.assertj.core.api.Assertions;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static org.conqat.lib.commons.collections.CollectionUtils.emptyList;
import static org.mockito.Mockito.mock;

/** Tests for the {@link JaCoCoTestwiseReportGenerator} class. */
public class JaCoCoTestwiseReportGeneratorTest extends CCSMTestCaseBase {

	/** Tests that the {@link JaCoCoTestwiseReportGenerator} produces the expected output. */
	@Test
	public void testSmokeTestTestwiseReportGeneration() throws Exception {
		String report = runGenerator("jacoco/cqddl/classes.zip", "jacoco/cqddl/coverage.exec");
		String expected = FileSystemUtils.readFileUTF8(useTestFile("jacoco/cqddl/report.json.expected"));
		Assertions.assertThat(report).isEqualToNormalizingWhitespace(expected);
	}

	/** Tests that the {@link JaCoCoTestwiseReportGenerator} produces the expected output. */
	@Test
	public void testSampleTestwiseReportGeneration() throws Exception {
		String report = runGenerator("jacoco/sample/classes.zip", "jacoco/sample/coverage.exec");
		String expected = FileSystemUtils.readFileUTF8(useTestFile("jacoco/sample/report.json.expected"));
		Assertions.assertThat(report).isEqualTo(expected);
	}

	/** Runs the report generator. */
	private String runGenerator(String testDataFolder, String execFileName) throws Exception {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(emptyList(), emptyList());
		TestwiseCoverage testwiseCoverage = new JaCoCoTestwiseReportGenerator(
				Collections.singletonList(classFileFolder),
				includeFilter, EDuplicateClassFileBehavior.IGNORE,
				mock(ILogger.class)).convert(useTestFile(execFileName));
		return ReportUtils.getReportAsString(generateDummyReportFrom(testwiseCoverage));
	}

	/** Generates a dummy coverage report object that wraps the given {@link TestwiseCoverage}. */
	public static TestwiseCoverageReport generateDummyReportFrom(TestwiseCoverage testwiseCoverage) {
		ArrayList<TestDetails> testDetails = new ArrayList<>();
		for (TestCoverageBuilder test : testwiseCoverage.getTests()) {
			testDetails.add(new TestDetails(test.getUniformPath(), "/path/to/source", "content"));
		}
		ArrayList<TestExecution> testExecutions = new ArrayList<>();
		for (TestCoverageBuilder test : testwiseCoverage.getTests()) {
			testExecutions.add(new TestExecution(test.getUniformPath(), test.getUniformPath().length(),
					ETestExecutionResult.PASSED));
		}
		return TestwiseCoverageReportBuilder.createFrom(testDetails, testwiseCoverage.getTests(), testExecutions);
	}
}