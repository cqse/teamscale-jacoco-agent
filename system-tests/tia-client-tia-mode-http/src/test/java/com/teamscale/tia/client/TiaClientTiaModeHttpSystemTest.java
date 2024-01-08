package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.test.commons.SystemTestUtils;
import org.junit.jupiter.api.Test;
import testframework.CustomTestFramework;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the {@link CustomTestFramework} from src/main with our agent attached in tia-mode=http (the agent is configured
 * in this project's build.gradle). The custom test framework contains an integration with the tia-client. Asserts that
 * the resulting {@link TestInfo}s look as expected.
 * <p>
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
public class TiaClientTiaModeHttpSystemTest {

	@Test
	public void systemTest() throws Exception {
		CustomTestFramework customTestFramework = new CustomTestFramework(SystemTestUtils.AGENT_PORT);
		customTestFramework.runTestsWithTia();

		assertThat(customTestFramework.testInfos.stream().map(SystemTestUtils::getCoverageString))
				.containsExactlyInAnyOrder("SystemUnderTest.java:4,13", "SystemUnderTest.java:4,8");
	}

}
