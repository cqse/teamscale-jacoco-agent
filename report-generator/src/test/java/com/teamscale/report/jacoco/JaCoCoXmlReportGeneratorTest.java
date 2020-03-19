package com.teamscale.report.jacoco;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import com.teamscale.test.TestDataBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.internal.data.CRC64;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
		String testFolderName = "empty-report-handling";
		long classId = calculateClassId(testFolderName, "TestClass.class");
		assertThatThrownBy(() -> runGenerator(testFolderName, EDuplicateClassFileBehavior.IGNORE,
				new ClasspathWildcardIncludeFilter("some.package.*", null), false, createDummyDump(classId)))
				.isExactlyInstanceOf(EmptyReportException.class);
	}

	@Test
	void testNonEmptyCoverageFileDoesNotThrowException() throws IOException, EmptyReportException {
		String testFolderName = "empty-report-handling";
		long classId = calculateClassId(testFolderName, "TestClass.class");
		runGenerator(testFolderName, EDuplicateClassFileBehavior.IGNORE,
				new ClasspathWildcardIncludeFilter("*", null), false, createDummyDump(classId));
	}

	@Test
	void testDisableEmptyCoverageException() throws IOException, EmptyReportException {
		// Should not throw EmptyReportException if we pass true for writingEmptyReports
		String testFolderName = "empty-report-handling";
		long classId = calculateClassId(testFolderName, "TestClass.class");
		runGenerator(testFolderName, EDuplicateClassFileBehavior.IGNORE,
				new ClasspathWildcardIncludeFilter("some.package.*", null), true, createDummyDump(classId));
	}

	/**
	 * Creates a dummy dump with the specified class ID. The class ID can currently be calculated with {@link
	 * org.jacoco.core.internal.data.CRC64#classId(byte[])}. This might change in the future, as it's considered an
	 * implementation detail of JaCoCo (c.f. <a href="https://www.jacoco.org/jacoco/trunk/doc/classids.html">
	 * https://www.jacoco.org/jacoco/trunk/doc/classids.html</a>)
	 */
	private static Dump createDummyDump(long classId) {
		ExecutionDataStore store = new ExecutionDataStore();
		store.put(new ExecutionData(classId, "TestClass", new boolean[]{true, true, true}));
		SessionInfo info = new SessionInfo("session-id", 124L, 125L);
		return new Dump(info, store);
	}

	/**
	 * Creates a dummy dump with an arbitrary class ID.
	 */
	private static Dump createDummyDump() {
		return createDummyDump(123);
	}

	private long calculateClassId(String testFolderName, String classFileName) throws IOException {
		File classFile = useTestFile(testFolderName + File.separator + classFileName);
		return CRC64.classId(Files.readAllBytes(classFile.toPath()));
	}

	/** Runs the report generator. */
	private void runGenerator(String testDataFolder,
							  EDuplicateClassFileBehavior duplicateClassFileBehavior) throws Exception, EmptyReportException {
		runGenerator(testDataFolder, duplicateClassFileBehavior, new ClasspathWildcardIncludeFilter(null, null), true,
				createDummyDump());
	}

	private void runGenerator(String testDataFolder,
							  EDuplicateClassFileBehavior duplicateClassFileBehavior,
							  ClasspathWildcardIncludeFilter filter,
							  boolean writeEmptyReportFiles,
							  Dump dump) throws IOException, EmptyReportException {
		File classFileFolder = useTestFile(testDataFolder);
		long currentTime = System.currentTimeMillis();
		String outputFilePath = "test-coverage-" + currentTime + ".xml";
		new JaCoCoXmlReportGenerator(Collections.singletonList(classFileFolder), filter,
				duplicateClassFileBehavior,
				mock(ILogger.class)).convert(dump, Paths.get(outputFilePath), writeEmptyReportFiles);
	}

}
