package eu.cqse.teamscale.report.testwise.jacoco;

import eu.cqse.teamscale.report.util.AntPatternIncludeFilter;
import eu.cqse.teamscale.report.util.ILogger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.conqat.lib.commons.collections.CollectionUtils.emptyList;
import static org.mockito.Mockito.mock;

/** Tests for the {@link TestwiseXmlReportGenerator} class. */
public class TestwiseXmlReportGeneratorTest extends CCSMTestCaseBase {

	/** Tests that the {@link TestwiseXmlReportGenerator} produces the expected output. */
	@Test
	public void testTestwiseReportGeneration() throws Exception {
		String report = runGenerator("jacoco/cqddl/classes.zip", "jacoco/cqddl/coverage.exec");
		String expected = FileSystemUtils.readFileUTF8(useTestFile("jacoco/cqddl/expected.xml"));
		assertEquals(expected, report);
	}

	/** Runs the report generator. */
	private String runGenerator(String testDataFolder, String execFileName) throws Exception {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(emptyList(), emptyList());
		return new TestwiseXmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter,
				mock(ILogger.class))
				.convertToString(useTestFile(execFileName));
	}
}