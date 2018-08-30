package eu.cqse.teamscale.report.testwise.closure;

import eu.cqse.teamscale.report.testwise.model.TestwiseCoverage;
import eu.cqse.teamscale.report.util.AntPatternIncludeFilter;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportUtils.getReportAsString;

/** Tests for {@link ClosureTestwiseCoverageGenerator}. */
@RunWith(JUnit4.class)
public class ClosureTestwiseCoverageGeneratorTest extends CCSMTestCaseBase {

	/** Tests that the JSON reports produce the expected result. */
	@Test
	@Ignore("Issue #1")
	public void testTestwiseReportGeneration() throws IOException {
		String actual = runGenerator("closure");
		String expected = FileSystemUtils.readFileUTF8(useTestFile("closure/expected.xml"));
		assertEquals(expected, actual);
	}

	/** Runs the report generator. */
	private String runGenerator(String closureCoverageFolder) throws IOException {
		File coverageFolder = useTestFile(closureCoverageFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(CollectionUtils.emptyList(),
				Arrays.asList("**/google-closure-library/**", "**.soy.generated.js", "soyutils_usegoog.js"));
		TestwiseCoverage testwiseCoverage = new ClosureTestwiseCoverageGenerator(
				Collections.singletonList(coverageFolder), includeFilter)
				.readTestCoverage();
		return getReportAsString(testwiseCoverage);
	}
}