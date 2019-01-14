package org.junit.platform.console.tasks;

import com.teamscale.client.TestDetails;
import com.teamscale.client.TestForPrioritization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		uniformPathToUniqueIdMapping.put(details.uniformPath, uniqueId);
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
	public List<String> convertToUniqueIds(List<TestForPrioritization> impactedTests) {
		return impactedTests.stream().map(test -> uniformPathToUniqueIdMapping.get(test.uniformPath))
				.collect(Collectors.toList());
	}
}
