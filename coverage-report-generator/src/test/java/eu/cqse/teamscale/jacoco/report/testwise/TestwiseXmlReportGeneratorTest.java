package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.util.AntPatternIncludeFilter;
import eu.cqse.teamscale.jacoco.util.ILogger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.conqat.lib.commons.collections.CollectionUtils.emptyList;
import static org.mockito.Mockito.mock;

public class TestwiseXmlReportGeneratorTest extends CCSMTestCaseBase {

	@Test
	public void testTestwiseReportGeneration() throws IOException {
		String report = runGenerator("jacoco/cqddl/classes.zip", "jacoco/cqddl/coverage.exec");
		assertEqualsIgnoreFormatting("jacoco/cqddl/expected.xml", report);
	}

	private void assertEqualsIgnoreFormatting(String expectedFileName, String actualReport) throws IOException {
		String expected = FileSystemUtils.readFileUTF8(useTestFile(expectedFileName));
		assertEquals(expected, actualReport);
	}

	/** Runs the report generator. */
	private String runGenerator(String testDataFolder, String execFileName) throws IOException {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(emptyList(), emptyList());
		return new TestwiseXmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter,
				mock(ILogger.class))
				.convert(useTestFile(execFileName));
	}
}