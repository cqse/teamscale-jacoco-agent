package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.cache.CoverageGenerationException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestwiseCoverage;
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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import static eu.cqse.teamscale.jacoco.report.testwise.TestwiseXmlReportUtils.writeReportToStream;

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
	public TestwiseXmlReportGenerator(List<File> codeDirectoriesOrArchives, Predicate<String> locationIncludeFilter, ILogger logger) throws CoverageGenerationException {
		this.executionDataReader = new CachingExecutionDataReader(logger);
		this.executionDataReader.analyzeClassDirs(codeDirectoriesOrArchives, locationIncludeFilter);
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

	/** Converts the given *.exec file to a XML report. */
	public String convert(File executionDataFile) throws IOException {
		return convert(readDumps(executionDataFile));
	}

	/** Converts the given dumps to a report. */
	public String convert(List<Dump> dumps) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		convertToReport(output, dumps);
		return output.toString(FileSystemUtils.UTF8_ENCODING);
	}

	/** Creates the testwise report. */
	private void convertToReport(OutputStream output, List<Dump> dumps) throws IOException {
		TestwiseCoverage testwiseCoverage = executionDataReader.buildCoverage(dumps);
		writeReportToStream(output, testwiseCoverage);
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