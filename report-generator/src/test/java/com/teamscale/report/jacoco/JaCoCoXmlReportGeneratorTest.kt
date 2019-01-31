package com.teamscale.report.jacoco;

import com.teamscale.report.jacoco.dump.Dump;
import com.teamscale.report.util.AntPatternIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Tests report generation with and without duplicate classes. */
public class JaCoCoXmlReportGeneratorTest extends CCSMTestCaseBase {

	/** Ensures that the normal case runs without exceptions. */
	@Test
	public void testNormalCaseThrowsNoException() throws Exception {
		runGenerator("no-duplicates", false);
	}

	/** Ensures that two identical duplicate classes do not cause problems. */
	@Test
	public void testIdenticalClassesShouldNotThrowException() throws Exception {
		runGenerator("identical-duplicate-classes", false);
	}

	/**
	 * Ensures that two non-identical, duplicate classes cause an exception to be
	 * thrown.
	 */
	@Test
	public void testDifferentClassesWithTheSameNameShouldThrowException() throws Exception {
		assertThatThrownBy(() -> runGenerator("different-duplicate-classes", false))
				.isExactlyInstanceOf(IOException.class).hasCauseExactlyInstanceOf(IllegalStateException.class);
	}

	/**
	 * Ensures that two non-identical, duplicate classes do not cause an exception
	 * to be thrown if the ignore-duplicates flag is set.
	 */
	@Test
	public void testDifferentClassesWithTheSameNameShouldNotThrowExceptionIfFlagIsSet() throws Exception {
		runGenerator("different-duplicate-classes", true);
	}

	/** Creates a dummy dump. */
	private static Dump createDummyDump() {
		ExecutionDataStore store = new ExecutionDataStore();
		store.put(new ExecutionData(123, "TestClass", new boolean[]{true, true, true}));
		SessionInfo info = new SessionInfo("session-id", 124L, 125L);
		return new Dump(info, store);
	}

	/** Runs the report generator. */
	private void runGenerator(String testDataFolder, boolean shouldIgnoreDuplicates) throws IOException {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(CollectionUtils.emptyList(),
				CollectionUtils.emptyList());
		new JaCoCoXmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter, shouldIgnoreDuplicates,
				mock(ILogger.class)).convert(createDummyDump());
	}

}
