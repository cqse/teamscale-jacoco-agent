package org.junit.platform.console.tasks;

import com.teamscale.client.TestDetails;
import com.teamscale.client.TestForPrioritization;
import com.teamscale.report.util.StringUtilsKt;
import org.junit.platform.console.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a list of test details that can currently be executed. Provides the ability to translate uniform paths
 * returned by the Teamscale server to unique IDs used in JUnit Platform.
 */
public class AvailableTests {

	/**
	 * A mapping from the tests uniform path (Teamscale internal representation) to
	 * unique id (JUnit internal representation).
	 */
	private Map<String, String> uniformPathToUniqueIdMapping = new HashMap<>();

	/** List of all test details. */
	private List<TestDetails> testList = new ArrayList<>();

	/** Adds a new {@link TestDetails} object and the according uniqueId. */
	public void add(String uniqueId, TestDetails details) {
		uniformPathToUniqueIdMapping.put(details.getUniformPath(), uniqueId);
		testList.add(details);
	}

	/** Returns the size of the test list. */
	public int size() {
		return testList.size();
	}

	/** Returns the list of available tests. */
	public List<TestDetails> getTestList() {
		return testList;
	}

	/** Returns whether the container holds any tests. */
	public boolean isEmpty() {
		return testList.isEmpty();
	}

	/**
	 * Converts the {@link TestForPrioritization}s returned from Teamscale to a
	 * list of unique IDs to be fed into JUnit Platform.
	 */
	public List<String> convertToUniqueIds(Logger logger, List<TestForPrioritization> impactedTests) {
		List<String> list = new ArrayList<>();
		for (TestForPrioritization impactedTest : impactedTests) {
			logger.info("" + impactedTest.getUniformPath() + " " + impactedTest.getSelectionReason());

			String testUniqueIds = uniformPathToUniqueIdMapping.get(impactedTest.getUniformPath());
			if (testUniqueIds == null) {
				logger.error("Retrieved invalid test '" + impactedTest.getUniformPath() + "' from Teamscale server!");
				logger.error("The following seem related:");
				uniformPathToUniqueIdMapping.keySet().stream().sorted(Comparator
						.comparing(testPath -> StringUtilsKt.editDistance(impactedTest.getUniformPath(), testPath)))
						.limit(5)
						.forEach(testAlternative -> logger.error(" - " + testAlternative));

				logger.error("Falling back to execute all...");
				return new ArrayList<>(uniformPathToUniqueIdMapping.values());
			}
			list.add(testUniqueIds);
		}
		return list;
	}
}
