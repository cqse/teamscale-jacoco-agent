package com.teamscale.profiler.installer

/**
 * Map of JVM-specific environment variables and their values. Also handles quoting to ensure the environment variables
 * are parsed correctly by the JVM.
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

	val etcEnvironmentLinesList
		/**
		 * Returns a list of lines that can be appended to /etc/environment. Lines are quoted if necessary.
		 */
		get() = sortedEntries()
			.map { entry -> "${entry.key}=${entry.value.quoteIfNecessary()}" }
			.toList()

	val systemdString
		/**
		 * Returns a string that can be used in the DefaultEnvironment setting of a global systemd config file. Entries are
		 * quoted if necessary.
		 */
		get() = sortedEntries()
			.joinToString(" ") { entry ->
				"${entry.key}=${entry.value}".quoteIfNecessary()
			}
}
