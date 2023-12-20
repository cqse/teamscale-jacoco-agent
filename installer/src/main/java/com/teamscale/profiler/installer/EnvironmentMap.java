package com.teamscale.profiler.installer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Map of environment variables and their values. */
public class EnvironmentMap {

	private final Map<String, String> environment = new HashMap<>();

	/**
	 * Creates a map of keys and values from the the supplied ones. Supplying e.g. A, B, C, D will store environment
	 * variables A=B and C=D.
	 */
	public EnvironmentMap(String... keysAndValues) {
		for (int i = 0; i < keysAndValues.length; i += 2) {
			environment.put(keysAndValues[i], keysAndValues[i + 1]);
		}
	}

	public Map<String, String> getMap() {
		return environment;
	}

	private String escape(String value) {
		return value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"");
	}

	private String quoteIfNecessary(String value) {
		String escaped = escape(value);
		if (escaped.equals(value) && !escaped.contains(" ")) {
			return escaped;
		}
		return "\"" + escaped + "\"";
	}

	private Stream<Map.Entry<String, String>> sortedEntryStream() {
		return environment.entrySet().stream().sorted(Map.Entry.comparingByKey());
	}

	/**
	 * Returns a string that can be appended to /etc/environment.
	 */
	public String getEtcEnvironmentString() {
		return sortedEntryStream().map(entry -> entry.getKey() + "=" + quoteIfNecessary(entry.getValue()))
				.collect(Collectors.joining("\n"));
	}

	/**
	 * Returns a list of lines that can be appended to /etc/environment.
	 */
	public List<String> getEtcEnvironmentLinesList() {
		return sortedEntryStream().map(entry -> entry.getKey() + "=" + quoteIfNecessary(entry.getValue())).toList();
	}

	/**
	 * Returns a string that can be used in the DefaultEnvironment setting of a global systemd config file.
	 */
	public String getSystemdString() {
		return sortedEntryStream().map(entry -> quoteIfNecessary(entry.getKey() + "=" + entry.getValue()))
				.collect(Collectors.joining(" "));
	}

}
