/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.util

import org.conqat.lib.commons.collections.CollectionUtils
import org.conqat.lib.commons.filesystem.AntPatternUtils
import org.conqat.lib.commons.filesystem.FileSystemUtils

import java.util.Arrays
import java.util.Collections
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Applies ANT include and exclude patterns to paths.
 */
class AntPatternIncludeFilter
/** Constructor.  */
    (locationIncludeFilters: List<String>, locationExcludeFilters: List<String>) : Predicate<String> {

    /** The include filters. Empty means include everything.  */
    private val locationIncludeFilters: List<Pattern>

    /** The exclude filters. Empty means exclude nothing.  */
    private val locationExcludeFilters: List<Pattern>

    init {
        this.locationIncludeFilters = CollectionUtils.map(
            locationIncludeFilters
        ) { filter -> AntPatternUtils.convertPattern(filter, false) }
        this.locationExcludeFilters = CollectionUtils.map(
            locationExcludeFilters
        ) { filter -> AntPatternUtils.convertPattern(filter, false) }
    }

    /** {@inheritDoc}  */
    override fun test(path: String): Boolean {
        return !isFiltered(FileSystemUtils.normalizeSeparators(path))
    }

    /**
     * Returns `true` if the given class file location (normalized to
     * forward slashes as path separators) should not be analyzed.
     *
     *
     * Exclude filters overrule include filters.
     */
    private fun isFiltered(location: String): Boolean {
        // first check includes
        return if (!locationIncludeFilters.isEmpty() && locationIncludeFilters.stream().noneMatch { filter ->
                filter.matcher(
                    location
                ).matches()
            }) {
            true
        } else locationExcludeFilters.stream().anyMatch { filter -> filter.matcher(location).matches() }
        // only if they match, check excludes
    }

}
