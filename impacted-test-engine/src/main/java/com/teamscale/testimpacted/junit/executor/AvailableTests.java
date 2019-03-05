package com.teamscale.testimpacted.junit.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.TestDetails;
import com.teamscale.client.TestForPrioritization;
import com.teamscale.testimpacted.junit.ImpactedTestEngine;
import org.conqat.lib.commons.string.StringUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.UniqueId;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestEngine.class);

	/**
	 * A mapping from the tests uniform path (Teamscale internal representation) to
	 * unique id (JUnit internal representation).
	 */
	private Map<String, UniqueId> uniformPathToUniqueIdMapping = new HashMap<>();

	/** List of all test details. */
	private List<ClusteredTestDetails> testList = new ArrayList<>();

	/** Adds a new {@link TestDetails} object and the according uniqueId. */
	public void add(UniqueId uniqueId, ClusteredTestDetails details) {
		uniformPathToUniqueIdMapping.put(details.uniformPath, uniqueId);
		testList.add(details);
	}

	/** Returns the size of the test list. */
	public int size() {
		return testList.size();
	}

	/** Returns the list of available tests. */
	public List<ClusteredTestDetails> getTestList() {
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
	public List<UniqueId> convertToUniqueIds(List<TestForPrioritization> impactedTests) {
		List<UniqueId> list = new ArrayList<>();
		for (TestForPrioritization impactedTest : impactedTests) {
			LOGGER.info(() -> impactedTest.uniformPath + " " + impactedTest.selectionReason);

			UniqueId testUniqueId = uniformPathToUniqueIdMapping.get(impactedTest.uniformPath);
			if (testUniqueId == null) {
				LOGGER.error(() ->"Retrieved invalid test '" + impactedTest.uniformPath + "' from Teamscale server!");
				LOGGER.error(() ->"The following seem related:");
				uniformPathToUniqueIdMapping.keySet().stream().sorted(Comparator
						.comparing(testPath -> StringUtils.editDistance(impactedTest.uniformPath, testPath))).limit(5)
						.forEach(testAlternative -> LOGGER.error(() -> " - " + testAlternative));

				LOGGER.error(() ->"Falling back to execute all...");
				return new ArrayList<>(uniformPathToUniqueIdMapping.values());
			}
			list.add(testUniqueId);
		}
		return list;
	}
}
