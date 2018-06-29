package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestCoverage;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Creates a XML report for an execution data store. The report is grouped by session.
 * <p>
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class TestwiseXmlReportGenerator {

	/** Directories and zip files that contain class files. */
	private Collection<File> codeDirectoriesOrArchives;

	/** Include filter to apply to all locations during class file traversal. */
	private Predicate<String> locationIncludeFilter;

	/**
	 * Create a new generator with a collection of class directories.
	 *
	 * @param codeDirectoriesOrArchives Root directory that contains the projects class files.
	 * @param locationIncludeFilter     Filter for class files
	 */
	public TestwiseXmlReportGenerator(List<File> codeDirectoriesOrArchives, Predicate<String> locationIncludeFilter) {
		this.codeDirectoriesOrArchives = codeDirectoriesOrArchives;
		this.locationIncludeFilter = locationIncludeFilter;
	}

	/** Creates the report. */
	public String convert(List<Dump> dumps) throws IOException {
		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			convertToReport(output, dumps);
			return output.toString(FileSystemUtils.UTF8_ENCODING);
		}
	}

	/** Creates the testwise report. */
	private void convertToReport(OutputStream output, List<Dump> dumps) throws IOException {
		CachingExecutionDataReader executionDataReader = new CachingExecutionDataReader();
		TestwiseXmlReportWriter writer = new TestwiseXmlReportWriter(output);

		executionDataReader.analyzeClassDirs(codeDirectoriesOrArchives, locationIncludeFilter);

		for (Dump dump: dumps) {
			String testId = dump.info.getId();
			if (testId.isEmpty()) {
				continue;
			}
			try {
				TestCoverage testCoverage = executionDataReader.buildCoverage(testId, dump.store);
				writer.writeTestCoverage(testCoverage);
			} catch (CoverageGenerationException e) {
				e.printStackTrace(); //TODO print to logger here
			}
		}

		writer.closeReport();
	}
}