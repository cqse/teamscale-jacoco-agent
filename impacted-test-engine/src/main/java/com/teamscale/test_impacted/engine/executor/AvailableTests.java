package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.utils.StringUtils;
import com.teamscale.client.TestDetails;
import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Holds a list of test details that can currently be executed. Provides the ability to translate uniform paths returned
 * by the Teamscale server to unique IDs used in JUnit Platform.
 */
public class AvailableTests {

	private static final Logger LOGGER = LoggerUtils.getLogger(AvailableTests.class);

	/**
	 * A mapping from the tests uniform path (Teamscale internal representation) to unique id (JUnit internal
	 * representation).
	 */
	private final Map<String, UniqueId> uniformPathToUniqueIdMapping = new HashMap<>();

	/** List of all test details. */
	private final List<ClusteredTestDetails> testList = new ArrayList<>();

	/** Adds a new {@link TestDetails} object and the according uniqueId. */
	public void add(UniqueId uniqueId, ClusteredTestDetails details) {
		uniformPathToUniqueIdMapping.put(details.uniformPath, uniqueId);
		testList.add(details);
	}

	/** Returns the list of available tests. */
	public List<ClusteredTestDetails> getTestList() {
		return testList;
	}

	/**
	 * Converts the {@link PrioritizableTest} to the {@link UniqueId} returned by the {@link TestEngine}.
	 */
	public Optional<UniqueId> convertToUniqueId(PrioritizableTest test) {
		UniqueId clusterUniqueId = uniformPathToUniqueIdMapping.get(test.testName);
		if (clusterUniqueId == null) {
			LOGGER.severe(() -> "Retrieved invalid test '" + test.testName + "' from Teamscale server!");
			LOGGER.severe(() -> "The following seem related:");
			uniformPathToUniqueIdMapping.keySet().stream().sorted(Comparator
							.comparing(testPath -> StringUtils.editDistance(test.testName, testPath))).limit(5)
					.forEach(testAlternative -> LOGGER.severe(() -> " - " + testAlternative));
		}
		return Optional.ofNullable(clusterUniqueId);
	}
}
