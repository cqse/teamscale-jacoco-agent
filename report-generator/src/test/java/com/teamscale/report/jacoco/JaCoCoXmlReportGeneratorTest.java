package com.teamscale.report.jacoco;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import com.teamscale.test.TestDataBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Tests report generation with and without duplicate classes. */
public class JaCoCoXmlReportGeneratorTest extends TestDataBase {

	/** Ensures that the normal case runs without exceptions. */
	@Test
	void testNormalCaseThrowsNoException() throws Exception {
		runGenerator("no-duplicates", EDuplicateClassFileBehavior.FAIL);
	}

	/** Ensures that two identical duplicate classes do not cause problems. */
	@Test
	void testIdenticalClassesShouldNotThrowException() throws Exception {
		runGenerator("identical-duplicate-classes", EDuplicateClassFileBehavior.FAIL);
	}

	/**
	 * Ensures that two non-identical, duplicate classes cause an exception to be thrown.
	 */
	@Test
	void testDifferentClassesWithTheSameNameShouldThrowException() {
		assertThatThrownBy(() -> runGenerator("different-duplicate-classes", EDuplicateClassFileBehavior.FAIL))
				.isExactlyInstanceOf(IOException.class).hasCauseExactlyInstanceOf(IllegalStateException.class);
	}

	/**
	 * Ensures that two non-identical, duplicate classes do not cause an exception to be thrown if the ignore-duplicates
	 * flag is set.
	 */
	@Test
	void testDifferentClassesWithTheSameNameShouldNotThrowExceptionIfFlagIsSet() throws Exception {
		runGenerator("different-duplicate-classes", EDuplicateClassFileBehavior.IGNORE);
	}

	@Test
	void testEmptyCoverageFileThrowsException() throws IOException {
		assertThatThrownBy(() -> runGenerator("empty-report-handling", EDuplicateClassFileBehavior.IGNORE,
				new ClasspathWildcardIncludeFilter("some.package.*", null), false))
				.isExactlyInstanceOf(EmptyReportException.class);
	}

	@Test
	void testNonEmptyCoverageFileDoesNotThrowException() throws IOException, EmptyReportException {
		runGenerator("empty-report-handling", EDuplicateClassFileBehavior.IGNORE,
				new ClasspathWildcardIncludeFilter("*", null), false);
	}

	@Test
	void testDisableEmptyCoverageException() throws IOException, EmptyReportException {
		// Should not throw EmptyReportException if we pass true for writingEmptyReports
		runGenerator("empty-report-handling", EDuplicateClassFileBehavior.IGNORE,
				new ClasspathWildcardIncludeFilter("some.package.*", null), true);
	}

	/** Creates a dummy dump. */
	private static Dump createDummyDump() {
		ExecutionDataStore store = new ExecutionDataStore();
		store.put(new ExecutionData(6270089523198553326L, "TestClass", new boolean[]{true, true, true}));
		SessionInfo info = new SessionInfo("session-id", 124L, 125L);
		return new Dump(info, store);
	}

	/** Runs the report generator. */
	private void runGenerator(String testDataFolder,
							  EDuplicateClassFileBehavior duplicateClassFileBehavior) throws Exception, EmptyReportException {
		runGenerator(testDataFolder, duplicateClassFileBehavior, new ClasspathWildcardIncludeFilter(null, null), true);
	}

	private void runGenerator(String testDataFolder,
							  EDuplicateClassFileBehavior duplicateClassFileBehavior,
							  ClasspathWildcardIncludeFilter filter,
							  boolean writeEmptyReportFiles) throws IOException, EmptyReportException {
		File classFileFolder = useTestFile(testDataFolder);
		long currentTime = System.currentTimeMillis();
		String outputFilePath = "test-coverage-" + currentTime + ".xml";
		new JaCoCoXmlReportGenerator(Collections.singletonList(classFileFolder), filter,
				duplicateClassFileBehavior,
				mock(ILogger.class)).convert(createDummyDump(), Paths.get(outputFilePath), writeEmptyReportFiles);
	}

}
