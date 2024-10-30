/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.util

import com.teamscale.client.AntPatternUtils
import com.teamscale.client.FileSystemUtils
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Applies ANT include and exclude patterns to paths.
 */
class AntPatternIncludeFilter(locationIncludeFilters: List<String?>, locationExcludeFilters: List<String?>) :
	Predicate<String> {
	/** The include filters. Empty means include everything.  */
	private val locationIncludeFilters: List<Pattern> =
		locationIncludeFilters.stream().map { filter: String? -> AntPatternUtils.convertPattern(filter, false) }
			.collect(Collectors.toList())

	/** The exclude filters. Empty means exclude nothing.  */
	private val locationExcludeFilters: List<Pattern> =
		locationExcludeFilters.stream().map { filter: String? -> AntPatternUtils.convertPattern(filter, false) }
			.collect(
				Collectors.toList()
			)

	/** {@inheritDoc}  */
	override fun test(path: String): Boolean {
		return !isFiltered(FileSystemUtils.normalizeSeparators(path))
	}

	/**
	 * Returns `true` if the given class file location (normalized to forward slashes as path separators)
	 * should not be analyzed.
	 *
	 *
	 * Exclude filters overrule include filters.
	 */
	private fun isFiltered(location: String): Boolean {
		// first check includes
		if (!locationIncludeFilters.isEmpty()
			&& locationIncludeFilters.stream().noneMatch { filter: Pattern -> filter.matcher(location).matches() }
		) {
			return true
		}
		// only if they match, check excludes
		return locationExcludeFilters.stream().anyMatch { filter: Pattern -> filter.matcher(location).matches() }
	}
}
