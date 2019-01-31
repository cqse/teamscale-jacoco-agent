package com.teamscale.report.util

import org.jacoco.core.runtime.WildcardMatcher
import org.jacoco.report.JavaNames
import java.util.function.Predicate

/***
 * Tests given class file paths against call name patterns.
 * E.g. "/some/file/path/test.jar@my/package/Test.class" matches "my/package/ *" or "my/package/Test"
 */
class ClasspathWildcardIncludeFilter
/**
 * Constructor.
 *
 * @param locationIncludeFilters Colon separated list of wildcard include patterns or null for no includes.
 * @param locationExcludeFilters Colon separated list of wildcard exclude patterns or null for no excludes.
 */
    (locationIncludeFilters: String?, locationExcludeFilters: String?) : Predicate<String> {

    /**
     * Include patterns to apply during JaCoCo's traversal of class files. If null
     * then everything is included.
     */
    private var locationIncludeFilters: WildcardMatcher? = null

    /**
     * Exclude patterns to apply during JaCoCo's traversal of class files. If null
     * then nothing is excluded.
     */
    private var locationExcludeFilters: WildcardMatcher? = null

    init {
        if (locationIncludeFilters != null && !locationIncludeFilters.isEmpty()) {
            this.locationIncludeFilters = WildcardMatcher(locationIncludeFilters)
        }
        if (locationExcludeFilters != null && !locationExcludeFilters.isEmpty()) {
            this.locationExcludeFilters = WildcardMatcher(locationExcludeFilters)
        }
    }

    override fun test(path: String): Boolean {
        val className = getClassName(path)
        // first check includes
        return if (locationIncludeFilters != null && !locationIncludeFilters!!.matches(className)) {
            false
        } else locationExcludeFilters == null || !locationExcludeFilters!!.matches(className)
        // if they match, check excludes
    }

    companion object {

        /**
         * Returns the normalized class name of the given class file's path.
         */
        /* package */ internal fun getClassName(path: String): String {
            val parts = FileSystemUtils.normalizeSeparators(path).split("@".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (parts.isEmpty()) {
                return ""
            }

            val pathInsideJar = parts[parts.size - 1]
            val pathWithoutExtension = pathInsideJar.substringBeforeLast('.')
            return JavaNames().getQualifiedClassName(pathWithoutExtension)
        }
    }
}
