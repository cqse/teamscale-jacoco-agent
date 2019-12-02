package com.teamscale.report.testwise.jacoco;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Creates a XML report for an execution data store. The report is grouped by session.
 * <p>
 * The class files under test must be compiled with debug information otherwise no coverage will be collected.
 */
public class JaCoCoTestwiseReportGenerator {

	/** The execution data reader and converter. */
	private CachingExecutionDataReader executionDataReader;

	/** The filter for the analyzed class files. */
	private final ClasspathWildcardIncludeFilter locationIncludeFilter;

	/**
	 * Create a new generator with a collection of class directories.
	 *
	 * @param codeDirectoriesOrArchives Root directory that contains the projects class files.
	 * @param locationIncludeFilter     Filter for class files
	 * @param logger                    The logger
	 */
	public JaCoCoTestwiseReportGenerator(Collection<File> codeDirectoriesOrArchives,
										 ClasspathWildcardIncludeFilter locationIncludeFilter,
										 EDuplicateClassFileBehavior duplicateClassFileBehavior,
										 ILogger logger) throws CoverageGenerationException {
		this.locationIncludeFilter = locationIncludeFilter;
		this.executionDataReader = new CachingExecutionDataReader(logger);
		this.executionDataReader
				.analyzeClassDirs(codeDirectoriesOrArchives, locationIncludeFilter, duplicateClassFileBehavior);
	}

	/** Converts the given dumps to a report. */
	public TestwiseCoverage convert(File executionDataFile) throws IOException {
		TestwiseCoverage testwiseCoverage = new TestwiseCoverage();
		CachingExecutionDataReader.DumpConsumer dumpConsumer = executionDataReader
				.buildCoverageConsumer(locationIncludeFilter, testwiseCoverage::add);
		readAndConsumeDumps(executionDataFile, dumpConsumer);
		return testwiseCoverage;
	}

	/** Converts the given dumps to a report. */
	public void convertAndConsume(File executionDataFile, Consumer<TestCoverageBuilder> consumer) throws IOException {
		CachingExecutionDataReader.DumpConsumer dumpConsumer = executionDataReader
				.buildCoverageConsumer(locationIncludeFilter, consumer);
		readAndConsumeDumps(executionDataFile, dumpConsumer);
	}

	/** Reads the dumps from the given *.exec file. */
	private void readAndConsumeDumps(File executionDataFile, Consumer<Dump> dumpConsumer) throws IOException {
		try (InputStream input = new BufferedInputStream(new FileInputStream(executionDataFile))) {
			ExecutionDataReader executionDataReader = new ExecutionDataReader(input);
			DumpCallback dumpCallback = new DumpCallback(dumpConsumer);
			executionDataReader.setExecutionDataVisitor(dumpCallback);
			executionDataReader.setSessionInfoVisitor(dumpCallback);
			executionDataReader.read();
			// Ensure that the last read dump is also consumed
			dumpCallback.processDump();
		}
	}

	/** Collects execution information per session and passes it to the consumer . */
	private static class DumpCallback implements IExecutionDataVisitor, ISessionInfoVisitor {

		/** The dump that is currently being read. */
		private Dump currentDump = null;

		/** The store to which coverage is currently written to. */
		private ExecutionDataStore store;

		/** The consumer to pass {@link Dump}s to. */
		private Consumer<Dump> dumpConsumer;

		private DumpCallback(Consumer<Dump> dumpConsumer) {
			this.dumpConsumer = dumpConsumer;
		}

		@Override
		public void visitSessionInfo(SessionInfo info) {
			processDump();
			store = new ExecutionDataStore();
			currentDump = new Dump(info, store);
		}

		@Override
		public void visitClassExecution(ExecutionData data) {
			store.put(data);
		}

		private void processDump() {
			if (currentDump != null) {
				dumpConsumer.accept(currentDump);
				currentDump = null;
			}
		}
	}
}