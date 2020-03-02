package com.teamscale.report.jacoco;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.xml.XMLFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/** Creates an XML report from binary execution data. */
public class JaCoCoXmlReportGenerator {

	/** The logger. */
	private final ILogger logger;

	/** Directories and zip files that contain class files. */
	private final List<File> codeDirectoriesOrArchives;

	/**
	 * Include filter to apply to all locations during class file traversal.
	 */
	private final ClasspathWildcardIncludeFilter locationIncludeFilter;

	/** Whether to ignore non-identical duplicates of class files. */
	private final EDuplicateClassFileBehavior duplicateClassFileBehavior;

	/** Part of the error message logged when validating the coverage report fails. */
	private static final String MOST_LIKELY_CAUSE_MESSAGE = "Most likely you did not configure the agent correctly." +
			" Please check that the includes and excludes options are set correctly so the relevant code is included." +
			" If in doubt, first include more code and then iteratively narrow the patterns down to just the relevant code." +
			" If you have specified the class-dir option, please make sure it points to a directory containing the" +
			" class files/jars/wars/ears/etc. for which you are trying to measure code coverage.";

	public JaCoCoXmlReportGenerator(List<File> codeDirectoriesOrArchives,
									ClasspathWildcardIncludeFilter locationIncludeFilter,
									EDuplicateClassFileBehavior duplicateClassFileBehavior, ILogger logger) {
		this.codeDirectoriesOrArchives = codeDirectoriesOrArchives;
		this.duplicateClassFileBehavior = duplicateClassFileBehavior;
		this.locationIncludeFilter = locationIncludeFilter;
		this.logger = logger;
	}


	/**
	 * Creates the report and writes it to a file.
	 *
	 * @return The file object of for the converted report or null if it could not be created
	 */
	public CoverageFile convert(Dump dump, Path filePath) throws IOException {
		File outputFile = createOutputFile(filePath);
		FileOutputStream output = new FileOutputStream(outputFile);
		convertToReport(output, dump);
		return new CoverageFile(outputFile);
	}

	private File createOutputFile(Path filePath) throws IOException {
		File outputFile = filePath.toFile();

		try {
			Files.createFile(outputFile.toPath());
		} catch (FileAlreadyExistsException e) {
			// Clear file contents
			new FileOutputStream(outputFile).close();
		} catch (IOException e) {
			throw new IOException("Could not create temporary coverage file" + outputFile.getAbsolutePath() + ". " +
					"This is used to cache the coverage file on disk before uploading it to its final destination. " +
					"This coverage is lost. Please fix the underlying issue to avoid losing coverage.", e);
		}

		return outputFile;
	}

	/** Creates the report. */
	private void convertToReport(OutputStream output, Dump dump) throws IOException {
		ExecutionDataStore mergedStore = dump.store;
		IBundleCoverage bundleCoverage = analyzeStructureAndAnnotateCoverage(mergedStore);
		sanityCheck(bundleCoverage);
		createReport(output, bundleCoverage, dump.info, mergedStore);
	}

	private void sanityCheck(IBundleCoverage coverage) {
		if (coverage.getPackages().size() == 0 || coverage.getLineCounter().getTotalCount() == 0) {
			logger.error("The generated coverage report is empty. " + MOST_LIKELY_CAUSE_MESSAGE);
		} else if (coverage.getLineCounter().getCoveredCount() == 0) {
			logger.error("The generated coverage report does not contain any covered source code lines. " +
					MOST_LIKELY_CAUSE_MESSAGE);
		}
	}

	/** Creates an XML report based on the given session and coverage data. */
	private static void createReport(OutputStream output, IBundleCoverage bundleCoverage, SessionInfo sessionInfo,
									 ExecutionDataStore store) throws IOException {
		XMLFormatter xmlFormatter = new XMLFormatter();
		IReportVisitor visitor = xmlFormatter.createVisitor(output);

		visitor.visitInfo(Collections.singletonList(sessionInfo), store.getContents());
		visitor.visitBundle(bundleCoverage, null);
		visitor.visitEnd();
	}

	/**
	 * Analyzes the structure of the class files in {@link #codeDirectoriesOrArchives} and builds an in-memory coverage
	 * report with the coverage in the given store.
	 */
	private IBundleCoverage analyzeStructureAndAnnotateCoverage(ExecutionDataStore store) throws IOException {
		CoverageBuilder coverageBuilder = new CoverageBuilder();
		if (duplicateClassFileBehavior != EDuplicateClassFileBehavior.FAIL) {
			coverageBuilder = new DuplicateIgnoringCoverageBuilder(this.logger,
					duplicateClassFileBehavior == EDuplicateClassFileBehavior.WARN);
		}

		Analyzer analyzer = new FilteringAnalyzer(store, coverageBuilder, locationIncludeFilter, logger);

		for (File file : codeDirectoriesOrArchives) {
			analyzer.analyzeAll(file);
		}

		return coverageBuilder.getBundle("dummybundle");
	}

}
