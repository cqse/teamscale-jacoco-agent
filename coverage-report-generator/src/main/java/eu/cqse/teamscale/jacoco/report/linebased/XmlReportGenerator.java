package eu.cqse.teamscale.jacoco.report.linebased;

import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.xml.XMLFormatter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Creates an XML report from binary execution data. */
public class XmlReportGenerator {

	/** Directories and zip files that contain class files. */
	private final List<File> codeDirectoriesOrArchives;

	/**
	 * Include filter to apply to all locations during class file traversal.
	 */
	private final Predicate<String> locationIncludeFilter;

	/** Whether to ignore non-identical duplicates of class files. */
	private final boolean ignoreNonidenticalDuplicateClassFiles;

	/** Constructor. */
	public XmlReportGenerator(List<File> codeDirectoriesOrArchives, Predicate<String> locationIncludeFilter,
							  boolean ignoreDuplicates) {
		this.codeDirectoriesOrArchives = codeDirectoriesOrArchives;
		this.ignoreNonidenticalDuplicateClassFiles = ignoreDuplicates;
		this.locationIncludeFilter = locationIncludeFilter;
	}

	/** Creates the report. */
	public String convert(Dump dump) throws IOException {
		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			convertToReport(output, dump);
			return output.toString(FileSystemUtils.UTF8_ENCODING);
		}
	}

	/** Creates the report. */
	public void convertToReport(OutputStream output, Dump dump) throws IOException {
		ExecutionDataStore mergedStore = dump.store;
		IBundleCoverage bundleCoverage = analyzeStructureAndAnnotateCoverage(mergedStore);
		createReport(output, bundleCoverage, dump.info, mergedStore);
	}

	/** Creates an XML report based on the given session and coverage data. */
	private static void createReport(OutputStream output, IBundleCoverage bundleCoverage, SessionInfo sessionInfo, ExecutionDataStore store) throws IOException {
		XMLFormatter xmlFormatter = new XMLFormatter();
		IReportVisitor visitor = xmlFormatter.createVisitor(output);

		visitor.visitInfo(Collections.singletonList(sessionInfo), store.getContents());
		visitor.visitBundle(bundleCoverage, null);
		visitor.visitEnd();
	}

	/**
	 * Analyzes the structure of the class files in
	 * {@link #codeDirectoriesOrArchives} and builds an in-memory coverage report
	 * with the coverage in the given store.
	 */
	private IBundleCoverage analyzeStructureAndAnnotateCoverage(ExecutionDataStore store) throws IOException {
		CoverageBuilder coverageBuilder = new CoverageBuilder();
		if (ignoreNonidenticalDuplicateClassFiles) {
			coverageBuilder = new DuplicateIgnoringCoverageBuilder();
		}

		Analyzer analyzer = new FilteringAnalyzer(store, coverageBuilder, locationIncludeFilter);

		for (File file: codeDirectoriesOrArchives) {
			analyzer.analyzeAll(file);
		}

		return coverageBuilder.getBundle("dummybundle");
	}

}
