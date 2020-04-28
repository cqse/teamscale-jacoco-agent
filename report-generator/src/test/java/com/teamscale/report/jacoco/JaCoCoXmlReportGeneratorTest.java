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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Tests report generation with and without duplicate classes. */
public class JaCoCoXmlReportGeneratorTest extends TestDataBase {

	/** Ensures that the normal case runs without exceptions. */
	@Test
	void testNormalCaseThrowsNoException() throws Exception {
		runGenerator("no-duplicates", EDuplicateClassFileBehavior.FAIL, false);
	}

	/** Ensures that two identical duplicate classes do not cause problems. */
	@Test
	void testIdenticalClassesShouldNotThrowException() throws Exception {
		runGenerator("identical-duplicate-classes", EDuplicateClassFileBehavior.FAIL, false);
	}

	/**
	 * Ensures that two non-identical, duplicate classes cause an exception to be thrown.
	 */
	@Test
	void testDifferentClassesWithTheSameNameShouldThrowException() {
		assertThatThrownBy(() -> runGenerator("different-duplicate-classes", EDuplicateClassFileBehavior.FAIL, false))
				.isExactlyInstanceOf(IOException.class).hasCauseExactlyInstanceOf(IllegalStateException.class);
	}

	/**
	 * Ensures that two non-identical, duplicate classes do not cause an exception to be thrown if the ignore-duplicates
	 * flag is set.
	 */
	@Test
	void testDifferentClassesWithTheSameNameShouldNotThrowExceptionIfFlagIsSet() throws Exception {
		runGenerator("different-duplicate-classes", EDuplicateClassFileBehavior.IGNORE, false);
	}

	/** Ensures that uncovered classes are removed from the report if flag is set. */
	@Test
	void testShrinking() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		runGenerator("no-duplicates", EDuplicateClassFileBehavior.FAIL, true).copy(stream);
		
		assertThat(stream.toString(StandardCharsets.UTF_8.name())).doesNotContain("<class");
	}
	
	/** Ensures that uncovered classes are contained in the report if flag is not set. */
	@Test
	void testNonShrinking() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		runGenerator("no-duplicates", EDuplicateClassFileBehavior.FAIL, true).copy(stream);
		
		assertThat(stream.toString(StandardCharsets.UTF_8.name())).contains("<class");
	}

	
	/** Creates a dummy dump. */
	private static Dump createDummyDump() {
		ExecutionDataStore store = new ExecutionDataStore();
		store.put(new ExecutionData(123, "TestClass", new boolean[]{true, true, true}));
		SessionInfo info = new SessionInfo("session-id", 124L, 125L);
		return new Dump(info, store);
	}

	/** Runs the report generator. */
	private CoverageFile runGenerator(String testDataFolder,
							  EDuplicateClassFileBehavior duplicateClassFileBehavior, boolean ignoreUncovered) throws IOException {
		File classFileFolder = useTestFile(testDataFolder);
		ClasspathWildcardIncludeFilter includeFilter = new ClasspathWildcardIncludeFilter(null, null);
		long currentTime = System.currentTimeMillis();
		String outputFilePath = "test-coverage-" + currentTime + ".xml";
		return new JaCoCoXmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter,
				duplicateClassFileBehavior, ignoreUncovered,
				mock(ILogger.class)).convert(createDummyDump(), Paths.get(outputFilePath));
	}
}
