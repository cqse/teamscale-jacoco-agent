package eu.cqse.teamscale.jacoco.client.report;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.junit.Test;

import eu.cqse.teamscale.jacoco.client.report.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.client.util.AntPatternIncludeFilter;
import eu.cqse.teamscale.jacoco.client.watch.IJacocoController.Dump;

/** Tests report generation with and without duplicate classes. */
public class XmlReportGeneratorTest extends CCSMTestCaseBase {

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
		assertThatThrownBy(() -> {
			runGenerator("different-duplicate-classes", false);
		}).isExactlyInstanceOf(IOException.class).hasCauseExactlyInstanceOf(IllegalStateException.class);
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
		store.put(new ExecutionData(123, "TestClass", new boolean[] { true, true, true }));
		SessionInfo info = new SessionInfo("session-id", 124l, 125l);
		return new Dump(store, info);
	}

	/** Runs the report generator. */
	private void runGenerator(String testDataFolder, boolean shouldIgnoreDuplicates) throws IOException {
		File classFileFolder = useTestFile(testDataFolder);
		AntPatternIncludeFilter includeFilter = new AntPatternIncludeFilter(CollectionUtils.emptyList(),
				CollectionUtils.emptyList());
		new XmlReportGenerator(Collections.singletonList(classFileFolder), includeFilter, shouldIgnoreDuplicates)
				.convert(createDummyDump());
	}

}
