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
		ServerOptions serverOptions = ServerOptions.builder()
				.url(propertyReader.getString("server.url"))
				.project(propertyReader.getString("server.project"))
				.userName(propertyReader.getString("server.userName"))
				.userAccessToken(propertyReader.getString("server.userAccessToken"))
				.build();

		return TestEngineOptions.builder()
				.serverOptions(serverOptions)
				.partition(propertyReader.getString("partition"))
				.runImpacted(propertyReader.getBoolean("runImpacted"))
				.runAllTests(propertyReader.getBoolean("runAllTests"))
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

		private String prefix;

		private PrefixingPropertyReader(String prefix, ConfigurationParameters configurationParameters) {
			this.prefix = prefix;
			this.configurationParameters = configurationParameters;
		}

		private <T> T get(String propertyName, Function<String, T> mapper) {
			return configurationParameters.get(prefix + propertyName).map(mapper).orElse(null);
		}

		private String getString(String propertyName) {
			return get(propertyName, Function.identity());
		}

		private Boolean getBoolean(String propertyName) {
			return get(propertyName, Boolean::valueOf);
		}

		private CommitDescriptor getCommitDescriptor(String propertyName) {
			return get(propertyName, CommitDescriptor::parse);
		}

		private List<String> getStringList(String propertyName) {
			return get(propertyName, listAsString -> {
				if (StringUtils.isEmpty(listAsString)) {
					return Collections.emptyList();
				}

				return Arrays.asList(listAsString.split(","));
			});
		}

		private Long getLong(String propertyName) {
			return get(propertyName, Long::valueOf);
		}
	}
}
