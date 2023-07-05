package com.teamscale.profiler.installer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EnvironmentMap {

	private Map<String, String> environment = new HashMap<>();

	public EnvironmentMap(String... keysAndValues) {
		for (int i = 0; i < keysAndValues.length; i += 2) {
			environment.put(keysAndValues[i], keysAndValues[i + 1]);
		}
	}

	private String escape(String value) {
		return value.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"");
	}

	private String quoteIfNecessary(String value) {
		String escaped = escape(value);
		if (escaped.equals(value) && !escaped.contains(" ")) {
			return escaped;
		}
		return "\"" + escaped + "\"";
	}

	public String getEtcEnvironmentString() {
		return environment.entrySet().stream().map(entry -> entry.getKey() + "=" + quoteIfNecessary(entry.getValue()))
				.collect(Collectors.joining("\n")) + "\n";
	}

	public String getSystemdString() {
		return environment.entrySet().stream().map(entry -> quoteIfNecessary(entry.getKey() + "=" + entry.getValue()))
				.collect(Collectors.joining(" "));
	}

}
