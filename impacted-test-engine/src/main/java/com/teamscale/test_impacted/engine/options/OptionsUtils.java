package com.teamscale.test_impacted.engine.options;

import com.teamscale.client.CommitDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

public class OptionsUtils {

	public static TestEngineOptions getEngineOptions(Properties properties) {
		PrefixingPropertyReader propertyReader = new PrefixingPropertyReader("teamscale.test.impacted.",
				properties);
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
				.baseline(propertyReader.getLong("baseline"))
				.agentUrls(propertyReader.getStringList("agentsUrls"))
				.reportDirectory(propertyReader.getString("reportDirectory"))
				.build();
	}

	private static class PrefixingPropertyReader {

		private final Properties properties;

		private String prefix;

		private PrefixingPropertyReader(String prefix, Properties properties) {
			this.prefix = prefix;
			this.properties = properties;
		}

		private <T> T get(String propertyName, Function<String, T> mapper) {
			String textValue = (String) properties.get(prefix + propertyName);
			if (textValue == null) {
				return null;
			}
			return mapper.apply(textValue);
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
			return get(propertyName, listAsString -> Arrays.asList(listAsString.split(",")));
		}

		private Long getLong(String propertyName) {
			return get(propertyName, Long::valueOf);
		}
	}
}
