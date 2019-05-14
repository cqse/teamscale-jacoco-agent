package com.teamscale.report.testwise.closure;

import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGeneratorTest;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.util.AntPatternIncludeFilter;
import com.teamscale.report.util.CommandLineLogger;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static com.teamscale.report.ReportUtils.getReportAsString;

/** Tests for {@link ClosureTestwiseCoverageGenerator}. */
@RunWith(JUnit4.class)
public class ClosureTestwiseCoverageGeneratorTest extends CCSMTestCaseBase {

	/** Tests that the JSON reports produce the expected result. */
	@Test
	public void testTestwiseReportGeneration() throws Exception {
		String actual = runGenerator("closure");
		JSONAssert.assertEquals(FileSystemUtils.readFileUTF8(useTestFile("closure/report.json.expected")), actual,
				JSONCompareMode.STRICT);
	}

	/** Runs the report generator. */
	private String runGenerator(String closureCoverageFolder) {
		File coverageFolder = useTestFile(closureCoverageFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(CollectionUtils.emptyList(),
				Arrays.asList("**/google-closure-library/**", "**.soy.generated.js", "soyutils_usegoog.js"));
		TestwiseCoverage testwiseCoverage = new ClosureTestwiseCoverageGenerator(
				Collections.singletonList(coverageFolder), includeFilter, new CommandLineLogger())
				.readTestCoverage();
		return getReportAsString(JaCoCoTestwiseReportGeneratorTest.generateDummyReportFrom(testwiseCoverage));
	}
}