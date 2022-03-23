package com.teamscale.test_impacted.engine.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import org.junit.platform.engine.ConfigurationParameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

/** Utility class for {@link TestEngineOptions}. */
public class TestEngineOptionUtils {

	/** Returns the {@link TestEngineOptions} configured in the {@link Properties}. */
	public static TestEngineOptions getEngineOptions(ConfigurationParameters configurationParameters) {
		PrefixingPropertyReader propertyReader = new PrefixingPropertyReader("teamscale.test.impacted.",
				configurationParameters);
		ServerOptions serverOptions = null;
		Boolean runImpacted = propertyReader.getBoolean("runImpacted", true);
		if (runImpacted) {
			serverOptions = ServerOptions.builder()
					.url(propertyReader.getString("server.url"))
					.project(propertyReader.getString("server.project"))
					.userName(propertyReader.getString("server.userName"))
					.userAccessToken(propertyReader.getString("server.userAccessToken"))
					.build();
		}

		return TestEngineOptions.builder()
				.serverOptions(serverOptions)
				.partition(propertyReader.getString("partition"))
				.runImpacted(runImpacted)
				.runAllTests(propertyReader.getBoolean("runAllTests", false))
				.includeAddedTests(propertyReader.getBoolean("includeAddedTests", true))
				.includeFailedAndSkipped(propertyReader.getBoolean("includeFailedAndSkipped", true))
				.endCommit(propertyReader.getCommitDescriptor("endCommit"))
				.baseline(propertyReader.getString("baseline"))
				.agentUrls(propertyReader.getStringList("agentsUrls"))
				.testEngineIds(propertyReader.getStringList("engines"))
				.reportDirectory(propertyReader.getString("reportDirectory"))
				.build();
	}

	/**
	 * Throws an {@link AssertionError} if the given value is blank.
	 */
	static void assertNotBlank(String value, String message) {
		if (StringUtils.isBlank(value)) {
			throw new AssertionError(message);
		}
	}

	/**
	 * Throws an {@link AssertionError} if the given value is null.
	 */
	static void assertNotNull(Object value, String message) {
		if (value == null) {
			throw new AssertionError(message);
		}
	}

	private static class PrefixingPropertyReader {

		private final ConfigurationParameters configurationParameters;

		private final String prefix;

		private PrefixingPropertyReader(String prefix, ConfigurationParameters configurationParameters) {
			this.prefix = prefix;
			this.configurationParameters = configurationParameters;
		}

		private <T> T getOrNull(String propertyName, Function<String, T> mapper) {
			return get(propertyName, mapper, null);
		}

		private <T> T get(String propertyName, Function<String, T> mapper, T defaultValue) {
			return configurationParameters.get(prefix + propertyName).map(mapper).orElse(defaultValue);
		}

		private String getString(String propertyName) {
			return getOrNull(propertyName, Function.identity());
		}

		private Boolean getBoolean(String propertyName, boolean defaultValue) {
			return get(propertyName, Boolean::valueOf, defaultValue);
		}

		private CommitDescriptor getCommitDescriptor(String propertyName) {
			return getOrNull(propertyName, CommitDescriptor::parse);
		}

		private List<String> getStringList(String propertyName) {
			return get(propertyName, listAsString -> {
				if (StringUtils.isEmpty(listAsString)) {
					return Collections.emptyList();
				}

				return Arrays.asList(listAsString.split(","));
			}, Collections.emptyList());
		}
	}
}
