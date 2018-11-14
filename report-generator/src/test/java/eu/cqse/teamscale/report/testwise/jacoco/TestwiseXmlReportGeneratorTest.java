package eu.cqse.teamscale.report.testwise.jacoco;

import eu.cqse.teamscale.report.testwise.model.ETestExecutionResult;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverageReport;
import eu.cqse.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import eu.cqse.teamscale.report.testwise.model.TestDetails;
import eu.cqse.teamscale.report.testwise.model.TestExecution;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverage;
import eu.cqse.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;
import eu.cqse.teamscale.report.util.AntPatternIncludeFilter;
import eu.cqse.teamscale.report.util.ILogger;
import org.assertj.core.api.Assertions;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static eu.cqse.teamscale.report.testwise.jacoco.TestwiseCoverageReportGenerator.getReportAsString;
import static org.conqat.lib.commons.collections.CollectionUtils.emptyList;
import static org.mockito.Mockito.mock;

/** Tests for the {@link TestwiseXmlReportGenerator} class. */
public class TestwiseXmlReportGeneratorTest extends CCSMTestCaseBase {

	/** Tests that the {@link TestwiseXmlReportGenerator} produces the expected output. */
	@Test
	public void testTestwiseReportGeneration() throws Exception {
		String report = runGenerator("jacoco/cqddl/classes.zip", "jacoco/cqddl/coverage.exec");
		String expected = FileSystemUtils.readFileUTF8(useTestFile("jacoco/cqddl/report.json.expected"));
		Assertions.assertThat(report).isEqualToNormalizingWhitespace(expected);
	}

	/** Runs the report generator. */
	private String runGenerator(String testDataFolder, String execFileName) throws Exception {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(emptyList(), emptyList());
		TestwiseCoverage testwiseCoverage = new TestwiseXmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter, true,
				mock(ILogger.class)).convert(useTestFile(execFileName));
		return getReportAsString(generateDummyReportFrom(testwiseCoverage));
	}

	public static TestwiseCoverageReport generateDummyReportFrom(TestwiseCoverage testwiseCoverage) {
		ArrayList<TestDetails> testDetails = new ArrayList<>();
		for (TestCoverageBuilder test : testwiseCoverage.getTests()) {
			testDetails.add(new TestDetails(test.getUniformPath(), "/path/to/source", "content"));
		}
		ArrayList<TestExecution> testExecutions = new ArrayList<>();
		for (TestCoverageBuilder test : testwiseCoverage.getTests()) {
			testExecutions.add(new TestExecution(test.getUniformPath(), test.getUniformPath().length(), ETestExecutionResult.PASSED));
		}
		return TestwiseCoverageReportBuilder.createFrom(testDetails, testwiseCoverage.getTests(), testExecutions);
	}
}