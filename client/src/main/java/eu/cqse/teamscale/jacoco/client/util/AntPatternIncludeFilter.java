/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.util;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.AntPatternUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

/**
 * Applies ANT include and exclude patterns to {@link Path}s.
 */
public class AntPatternIncludeFilter implements Predicate<Path> {

	/** The include filters. Empty means include everything. */
	private final List<Pattern> locationIncludeFilters;

	/** The exclude filterns. Empty means exclude nothing. */
	private final List<Pattern> locationExcludeFilters;

	/** Constructor. */
	public AntPatternIncludeFilter(List<String> locationIncludeFilters, List<String> locationExcludeFilters) {
		this.locationIncludeFilters = CollectionUtils.map(locationIncludeFilters,
				filter -> AntPatternUtils.convertPattern(filter, false));
		this.locationExcludeFilters = CollectionUtils.map(locationExcludeFilters,
				filter -> AntPatternUtils.convertPattern(filter, false));
	}

	/** {@inheritDoc} */
	@Override
	public boolean test(Path path) {
		return !isFiltered(FileSystemUtils.normalizeSeparators(path.toString()));
	}

	/**
	 * Returns <code>true</code> if the given class file location (normalized to
	 * forward slashes as path separators) should not be analyzed.
	 * 
	 * Exclude filters overrule include filters.
	 */
	private boolean isFiltered(String location) {
		// first check includes
		if (!locationIncludeFilters.isEmpty()
				&& locationIncludeFilters.stream().noneMatch(filter -> filter.matcher(location).matches())) {
			return true;
		}
		// only if they match, check excludes
		return locationExcludeFilters.stream().anyMatch(filter -> filter.matcher(location).matches());
	}

}
