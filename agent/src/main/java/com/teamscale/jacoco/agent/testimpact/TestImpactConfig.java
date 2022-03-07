package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
import com.teamscale.jacoco.agent.options.ETestwiseCoverageMode;

/** Config necessary for TIA. */
public class TestImpactConfig {

	/**
	 * How testwise coverage should be handled in test-wise mode.
	 */
	public ETestwiseCoverageMode testwiseCoverageMode = ETestwiseCoverageMode.EXEC_FILE;

	/**
	 * The name of the environment variable that holds the test uniform path for TIA mode.
	 */
	public String testEnvironmentVariable = null;

	/**
	 * Handles all TIA-related command line option.
	 *
	 * @return true if it has successfully processed the given option.
	 */
	public static boolean handleTiaOptions(TestImpactConfig options, String key,
										   String value) throws AgentOptionParseException {
		switch (key) {
			case "tia-mode":
				options.testwiseCoverageMode = AgentOptionsParser.parseEnumValue(key, value,
						ETestwiseCoverageMode.class);
				return true;
			case "test-env":
				options.testEnvironmentVariable = value;
				return true;
			default:
				return false;
		}
	}


}
