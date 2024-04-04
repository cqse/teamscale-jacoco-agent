package com.teamscale.profiler.installer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Map of JVM-specific environment variables and their values. Also handles quoting to ensure the environment variables
 * are parsed correctly by the JVM.
 */
public class JvmEnvironmentMap {

	private final Map<String, String> environment = new HashMap<>();

	/**
	 * Creates a map of keys and values from the supplied ones. Supplying e.g. A, B, C, D will store environment
	 * variables A=B and C=D.
	 */
	public JvmEnvironmentMap(String... keysAndValues) {
		for (int i = 0; i < keysAndValues.length; i += 2) {
			environment.put(keysAndValues[i], keysAndValues[i + 1]);
		}
	}

	/**
	 * Returns a map of environment variable names to their values. Values are quoted if necessary.
	 */
	public Map<String, String> getEnvironmentVariableMap() {
		Map<String, String> result = new HashMap<>();
		for (String key : environment.keySet()) {
			result.put(key, quoteIfNecessary(environment.get(key)));
		}
		return result;
	}

	/**
	 * The java command-line will treat single and double quotes as quoting and spaces as argument separators. All other
	 * characters are treated verbatim, including backslash. We simply assume that no quotes are used in the environment
	 * variable values. Therefore, we only need to quote arguments that contain spaces.
	 */
	private String quoteIfNecessary(String value) {
		if (!value.contains(" ")) {
			return value;
		}
		return "\"" + value + "\"";
	}

	private Stream<Map.Entry<String, String>> sortedEntryStream() {
		return environment.entrySet().stream().sorted(Map.Entry.comparingByKey());
	}

	/**
	 * Returns a list of lines that can be appended to /etc/environment. Lines are quoted if necessary.
	 */
	public List<String> getEtcEnvironmentLinesList() {
		return sortedEntryStream().map(entry -> entry.getKey() + "=" + quoteIfNecessary(entry.getValue())).toList();
	}

	/**
	 * Returns a string that can be used in the DefaultEnvironment setting of a global systemd config file. Entries are
	 * quoted if necessary.
	 */
	public String getSystemdString() {
		return sortedEntryStream().map(entry -> quoteIfNecessary(entry.getKey() + "=" + entry.getValue()))
				.collect(Collectors.joining(" "));
	}

}
