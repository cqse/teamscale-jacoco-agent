package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.TestInfo;
import org.junit.jupiter.api.Test;
import testframework.CustomTestFramework;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the {@link CustomTestFramework} from src/main with our agent attached in tia-mode=http (the agent is configured
 * in this project's build.gradle). The custom test framework contains an integration with the tia-client. Asserts that
 * the resulting {@link TestInfo}s look as expected.
 * <p>
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
public class TiaClientTiaModeHttpSystemTest {

	/** This port must match what is configured for the -javaagent line in this project's build.gradle. */
	private final int agentPort = 65433;

	@Test
	public void systemTest() throws Exception {
		CustomTestFramework customTestFramework = new CustomTestFramework(agentPort);
		customTestFramework.runTestsWithTia();

		assertThat(customTestFramework.testInfos.stream().map(TiaClientTiaModeHttpSystemTest::getCoverageString))
				.containsExactlyInAnyOrder("SystemUnderTest.java:4,13", "SystemUnderTest.java:4,8");
	}

	private static String getCoverageString(TestInfo info) {
		return info.paths.stream().flatMap(path -> path.getFiles().stream())
				.map(file -> file.fileName + ":" + file.coveredLines).collect(
						Collectors.joining(";"));
	}

}
