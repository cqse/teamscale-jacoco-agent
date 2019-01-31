package com.teamscale.report.util

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Wrapper around ConQAT ANT pattern utils to make it accessible from the other modules.
 */
object AntPatternUtils {

    /** Converts an ANT pattern to a regex pattern.  */
    @Throws(PatternSyntaxException::class)
    fun convertPattern(antPattern: String, caseSensitive: Boolean): Pattern {
        return org.conqat.lib.commons.filesystem.AntPatternUtils.convertPattern(antPattern, caseSensitive)
    }
}