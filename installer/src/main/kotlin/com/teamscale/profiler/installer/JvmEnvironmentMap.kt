package com.teamscale.profiler.installer

/**
 * Represents a mapping of JVM environment variables.
 * This class allows flexible handling of environment variable mappings, including formatting for use in
 * various contexts such as `/etc/environment` and systemd configurations.
 *
 * @constructor Initializes the environment map using the provided key-value pairs. Each pair of elements in
 * `keysAndValues` represents an environment variable and its corresponding value. For example, providing arguments
 * "A", "B", "C", "D" will construct an environment map containing A=B and C=D.
 *
 * @param keysAndValues A vararg list of strings representing keys and their corresponding values. Each pair of strings
 * must have a key followed by its value.
 */
class JvmEnvironmentMap(vararg keysAndValues: String) {
	private val environment = mutableMapOf<String, String>()

	/**
	 * Creates a map of keys and values from the supplied ones. Supplying e.g. A, B, C, D will store environment
	 * variables A=B and C=D.
	 */
	init {
		var i = 0
		while (i < keysAndValues.size) {
			environment[keysAndValues[i]] = keysAndValues[i + 1]
			i += 2
		}
	}

	val environmentVariableMap
		/**
		 * Returns a map of environment variable names to their values. Values are quoted if necessary.
		 */
		get() = environment.mapValues { it.value.quoteIfNecessary() }

	/**
	 * The java command-line will treat single and double quotes as quoting and spaces as argument separators. All other
	 * characters are treated verbatim, including backslash. We simply assume that no quotes are used in the environment
	 * variable values. Therefore, we only need to quote arguments that contain spaces.
	 */
	private fun String.quoteIfNecessary() =
		if (!contains(" ")) this else "\"${this}\""

	private fun sortedEntries() =
		environment.entries.sortedBy { it.key }

	/** Returns a list of lines that can be appended to /etc/environment. Lines are quoted if necessary. */
	val etcEnvironmentLinesList
		get() = sortedEntries()
			.map { entry -> "${entry.key}=${entry.value.quoteIfNecessary()}" }
			.toList()

	/**
	 * Returns a string that can be used in the DefaultEnvironment setting of a global systemd config file. Entries are
	 * quoted if necessary.
	 */
	val systemdString
		get() = sortedEntries()
			.joinToString(" ") { entry ->
				"${entry.key}=${entry.value}".quoteIfNecessary()
			}
}
