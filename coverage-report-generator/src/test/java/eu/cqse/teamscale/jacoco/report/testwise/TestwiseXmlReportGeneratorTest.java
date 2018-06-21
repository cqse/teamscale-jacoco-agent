package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.util.AntPatternIncludeFilter;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestwiseXmlReportGeneratorTest extends CCSMTestCaseBase {

	@Test
	public void testTestwiseReportGeneration() throws IOException {
		String report = runGenerator("jacoco/cqddl/classes.zip", "jacoco/cqddl/coverage.exec");
		assertEqualsIgnoreFormatting("jacoco/cqddl/expected.xml", report);
	}

	private void assertEqualsIgnoreFormatting(String expectedFileName, String actualReport) throws IOException {
		String expected = FileSystemUtils.readFileUTF8(useTestFile(expectedFileName));
		assertEquals(expected, actualReport);
	}

	/** Reads the dumps from the given *.exec file. */
	private List<Dump> readDumps(String execFileName) throws IOException {
		FileInputStream input = new FileInputStream(useTestFile(execFileName));
		ExecutionDataReader executionDataReader = new ExecutionDataReader(input);
		DumpCollector dumpCollector = new DumpCollector();
		executionDataReader.setExecutionDataVisitor(dumpCollector);
		executionDataReader.setSessionInfoVisitor(dumpCollector);
		executionDataReader.read();
		return dumpCollector.dumps;
	}

	/** Runs the report generator. */
	private String runGenerator(String testDataFolder, String execFileName) throws IOException {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(CollectionUtils.emptyList(),
				CollectionUtils.emptyList());
		return new TestwiseXmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter)
				.convert(readDumps(execFileName));
	}

	private class DumpCollector implements IExecutionDataVisitor, ISessionInfoVisitor {
		public List<Dump> dumps = new ArrayList<>();

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