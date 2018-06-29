package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestCoverage;
import eu.cqse.teamscale.jacoco.util.ILogger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Creates a XML report for an execution data store. The report is grouped by session.
 * <p>
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class TestwiseXmlReportGenerator {

	/** The logger. */
	private final ILogger logger;

	/** The execution data reader and converter. */
	private CachingExecutionDataReader executionDataReader;

	/**
	 * Create a new generator with a collection of class directories.
	 *
	 * @param codeDirectoriesOrArchives Root directory that contains the projects class files.
	 * @param locationIncludeFilter     Filter for class files
	 * @param logger                    The logger
	 */
	public TestwiseXmlReportGenerator(List<File> codeDirectoriesOrArchives, Predicate<String> locationIncludeFilter, ILogger logger) {
		this.executionDataReader = new CachingExecutionDataReader(logger);
		this.executionDataReader.analyzeClassDirs(codeDirectoriesOrArchives, locationIncludeFilter);
		this.logger = logger;
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

	/** Reads the dumps from the given *.exec file. */
	public String convert(File executionDataFile) throws IOException {
		return convert(readDumps(executionDataFile));
	}

	/** Creates the report. */
	public String convert(List<Dump> dumps) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		convertToReport(output, dumps);
		return output.toString(FileSystemUtils.UTF8_ENCODING);
	}

	/** Creates the testwise report. */
	private void convertToReport(OutputStream output, List<Dump> dumps) throws IOException {
		TestwiseXmlReportWriter writer = new TestwiseXmlReportWriter(output);
		for (Dump dump : dumps) {
			String testId = dump.info.getId();
			if (testId.isEmpty()) {
				continue;
			}
			try {
				TestCoverage testCoverage = executionDataReader.buildCoverage(testId, dump.store);
				writer.writeTestCoverage(testCoverage);
			} catch (CoverageGenerationException e) {
				logger.error(e);
			}
		}

		writer.closeReport();
	}

	/** Collects dumps per session. */
	private class DumpCollector implements IExecutionDataVisitor, ISessionInfoVisitor {

		/** List of dumps. */
		public List<Dump> dumps = new ArrayList<>();

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