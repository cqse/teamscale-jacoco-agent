package eu.cqse.teamscale.jacoco.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.AntPatternUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.xml.XMLFormatter;

import eu.cqse.teamscale.jacoco.client.util.Benchmark;
import eu.cqse.teamscale.jacoco.client.watch.IJacocoController.Dump;

/** Creates an XML report from binary execution data. */
public class XmlReportGenerator {

	/** Directories and zip files that contain class files. */
	private final List<File> codeDirectoriesOrArchives;

	/**
	 * Ant-style include filters to apply to all locations during class file
	 * traversal.
	 */
	private final List<Pattern> locationIncludeFilters;

	/**
	 * Ant-style exclude filters to apply to all locations during class file
	 * traversal.
	 */
	private final List<Pattern> locationExcludeFilters;

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Whether to ignore non-identical duplicates of class files. */
	private final boolean ignoreNonidenticalDuplicateClassFiles;

	/**
	 * Constructor.
	 */
	public XmlReportGenerator(List<File> codeDirectoriesOrArchives, List<String> locationIncludeFilters,
			List<String> locationExcludeFilters, boolean ignoreDuplicates) {
		this.codeDirectoriesOrArchives = codeDirectoriesOrArchives;
		this.ignoreNonidenticalDuplicateClassFiles = ignoreDuplicates;
		this.locationIncludeFilters = CollectionUtils.map(locationIncludeFilters,
				filter -> AntPatternUtils.convertPattern(filter, false));
		this.locationExcludeFilters = CollectionUtils.map(locationExcludeFilters,
				filter -> AntPatternUtils.convertPattern(filter, false));
	}

	/**
	 * Creates the report.
	 */
	public String convert(Dump dump) throws IOException {
		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			IBundleCoverage bundleCoverage = analyzeStructureAndAnnotateCoverage(dump.store);
			return createReport(bundleCoverage, dump);
		}
	}

	/** Creates an XML report based on the given session and coverage data. */
	private static String createReport(IBundleCoverage bundleCoverage, Dump dump) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		XMLFormatter xmlFormatter = new XMLFormatter();
		IReportVisitor visitor = xmlFormatter.createVisitor(output);

		visitor.visitInfo(Collections.singletonList(dump.info), dump.store.getContents());
		visitor.visitBundle(bundleCoverage, null);
		visitor.visitEnd();

		return output.toString(FileSystemUtils.UTF8_ENCODING);
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

		Analyzer analyzer = new Analyzer(store, coverageBuilder) {
			@Override
			public int analyzeAll(InputStream input, String location) throws IOException {
				// we must normalize first since ANT patterns assume forward slashes
				String normalizedLocation = FileSystemUtils.normalizeSeparators(location);
				if (location.endsWith(".class") && isFiltered(normalizedLocation)) {
					logger.debug("Filtering class file {}", normalizedLocation);
					return 0;
				}
				return super.analyzeAll(input, location);
			}
		};

		for (File file : codeDirectoriesOrArchives) {
			analyzer.analyzeAll(file);
		}

		return coverageBuilder.getBundle("dummybundle");
	}

	/** Modified {@link CoverageBuilder} that ignores non-identical duplicates. */
	private static class DuplicateIgnoringCoverageBuilder extends CoverageBuilder {

		/** The logger. */
		private final Logger logger = LogManager.getLogger(this);

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visitCoverage(IClassCoverage coverage) {
			try {
				super.visitCoverage(coverage);
			} catch (IllegalStateException e) {
				logger.warn("Ignoring duplicate, non-identical class file for class {}", coverage.getName(), e);
			}
		}

	}

	/**
	 * Returns <code>true</code> if the given class file location (normalized to
	 * forward slashes as path separators) should not be analyzed.
	 * 
	 * Exclude filters overrule include filters.
	 */
	private boolean isFiltered(String location) {
		if (!locationIncludeFilters.isEmpty()
				&& locationIncludeFilters.stream().noneMatch(filter -> filter.matcher(location).matches())) {
			return true;
		}
		return locationExcludeFilters.stream().anyMatch(filter -> filter.matcher(location).matches());
	}

}
