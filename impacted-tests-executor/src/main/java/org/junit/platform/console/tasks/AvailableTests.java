package org.junit.platform.console.tasks;

import com.teamscale.client.TestDetails;
import com.teamscale.client.TestForPrioritization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AvailableTests {

	/**
	 * A mapping from the tests uniform path (Teamscale internal representation) to
	 * unique id (JUnit internal representation).
	 */
	private Map<String, String> uniformPathToUniqueIdMapping = new HashMap<>();

	/** List of all test details. */
	private List<TestDetails> testList = new ArrayList<>();

	public void add(String uniqueId, TestDetails details) {
		uniformPathToUniqueIdMapping.put(details.uniformPath, uniqueId);
		testList.add(details);
	}

	public int size() {
		return testList.size();
	}

	public List<TestDetails> getList() {
		return testList;
	}

	public boolean isEmpty() {
		return testList.isEmpty();
	}

	public List<String> convertToUniqueIds(List<TestForPrioritization> impactedTests) {
		return impactedTests.stream().map(test -> uniformPathToUniqueIdMapping.get(test.uniformPath))
				.collect(Collectors.toList());
	}
}
