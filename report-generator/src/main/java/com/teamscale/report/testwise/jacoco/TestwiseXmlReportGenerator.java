package com.teamscale.report.testwise.jacoco;

import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Creates a XML report for an execution data store. The report is grouped by session.
 * <p>
 * The class files under test must be compiled with debug information otherwise no coverage will be collected.
 */
public class TestwiseXmlReportGenerator {

	/** The execution data reader and converter. */
	private CachingExecutionDataReader executionDataReader;

	/**
	 * Create a new generator with a collection of class directories.
	 *
	 * @param codeDirectoriesOrArchives Root directory that contains the projects class files.
	 * @param locationIncludeFilter     Filter for class files
	 * @param logger                    The logger
	 */
	public TestwiseXmlReportGenerator(Collection<File> codeDirectoriesOrArchives, Predicate<String> locationIncludeFilter, boolean ignoreNonidenticalDuplicateClassFiles, ILogger logger) throws CoverageGenerationException {
		this.executionDataReader = new CachingExecutionDataReader(logger);
		this.executionDataReader.analyzeClassDirs(codeDirectoriesOrArchives, locationIncludeFilter, ignoreNonidenticalDuplicateClassFiles);
	}

	/** Converts the given dumps to a report. */
	public TestwiseCoverage convert(Collection<File> executionDataFiles) throws IOException {
		TestwiseCoverage aggregatedTestwiseCoverage = new TestwiseCoverage();
		for (File executionDataFile : executionDataFiles) {
			aggregatedTestwiseCoverage.add(executionDataReader.buildCoverage(readDumps(executionDataFile)));
		}
		return aggregatedTestwiseCoverage;
	}

	/** Converts the given dumps to a report. */
	public TestwiseCoverage convert(File executionDataFile) throws IOException {
		return executionDataReader.buildCoverage(readDumps(executionDataFile));
	}

	/** Reads the dumps from the given *.exec file. */
	private List<Dump> readDumps(File executionDataFile) throws IOException {
		FileInputStream input = new FileInputStream(executionDataFile);
		ExecutionDataReader executionDataReader = new ExecutionDataReader(input);
		DumpCollector dumpCollector = new DumpCollector();
		executionDataReader.setExecutionDataVisitor(dumpCollector);
		executionDataReader.setSessionInfoVisitor(dumpCollector);
		executionDataReader.read();
		return dumpCollector.dumps;
	}

	/** Collects dumps per session. */
	private class DumpCollector implements IExecutionDataVisitor, ISessionInfoVisitor {

		/** List of dumps. */
		public final List<Dump> dumps = new ArrayList<>();

		/** The store to which coverage is currently written to. */
		private ExecutionDataStore store;

		@Override
		public void visitSessionInfo(SessionInfo info) {
			store = new ExecutionDataStore();
			dumps.add(new Dump(info, store));
		}

		@Override
		public void visitClassExecution(ExecutionData data) {
			store.put(data);
		}
	}
}