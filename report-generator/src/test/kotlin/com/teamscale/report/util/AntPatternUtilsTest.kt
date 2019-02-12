/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright 2005-2011 the ConQAT Project                                   |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
+-------------------------------------------------------------------------*/
package com.teamscale.report.util

import org.conqat.lib.commons.test.CCSMTestCaseBase
import org.junit.Test

import java.util.regex.Pattern

/**
 * Test for [AntPatternUtils]. The test cases mainly ensure that the Java
 * and .NET implementations of [AntPatternUtils] produce the same results
 * despite different implementations of Regex.
 */
class AntPatternUtilsTest : CCSMTestCaseBase() {

    /**
     * Tests matching files by stating an arbitrary folder and an extension
     * using the doublestar-dot-extension syntax.
     */
    @Test
    fun testRecursiveMatchByExtension() {
        val pattern = getPattern("**.java")

        assertMatch(pattern, "foo.java")
        assertMatch(pattern, "foo.bar.java")
        assertMatch(pattern, "foo/bar.java")
        assertMatch(pattern, "foo/bar/baz.java")

        assertNoMatch(pattern, "foo.bar")
        assertNoMatch(pattern, "foo.javabar")
    }

    /** Tests matching files by stating an arbitrary folder and an extension.  */
    @Test
    fun testRecursiveMatchByPathAndExtension() {
        val pattern = getPattern("**/*.cs")

        assertMatch(pattern, "foo.cs")
        assertMatch(pattern, "foo/bar.cs")
        assertMatch(pattern, "foo/bar/baz.cs")

        assertNoMatch(pattern, "foo.bar")
        assertNoMatch(pattern, "foo.csd")
    }

    /** Tests matching files at the directory root level.  */
    @Test
    fun testNonRecursiveMatchByExtension() {
        val pattern = getPattern("*.java")

        assertMatch(pattern, "foo.java")
        assertNoMatch(pattern, "foo/bar.java")
        assertNoMatch(pattern, "foo/bar/baz.java")

        assertNoMatch(pattern, "foo.bar")
        assertNoMatch(pattern, "foo.javabar")
    }

    /** Tests case insensitive matching.  */
    @Test
    fun testCaseInsensitivity() {
        val pattern = getPattern("*.java")

        assertMatch(pattern, "foo.java")
        assertMatch(pattern, "foo.JaVa")
    }

    /** Tests matching with the tail dot syntax.  */
    @Test
    fun testTailDot1() {
        val pattern = getPattern("*.")

        assertMatch(pattern, "foo")
        assertMatch(pattern, "foo.")
        assertNoMatch(pattern, "foo.xml")
        assertNoMatch(pattern, ".xml")
    }

    /** Tests matching with the tail dot syntax.  */
    @Test
    fun testTailDot2() {
        val pattern = getPattern("**/foo/*.")

        assertNoMatch(pattern, "foo")
        assertMatch(pattern, "foo/bar")
        assertNoMatch(pattern, "foo/bar.txt")
        assertNoMatch(pattern, "foo.xml")
    }

    /** Tests matching with the tail dot syntax.  */
    @Test
    fun testTailDot3() {
        val pattern = getPattern("foo/**.")

        assertNoMatch(pattern, "foo")
        assertMatch(pattern, "foo/bar")
        assertMatch(pattern, "foo/bar/baz")
        assertMatch(pattern, "foo/bar.dir/baz")
        assertNoMatch(pattern, "foo/baz/bar.txt")
    }

    /** Asserts that the given pattern matches.  */
    private fun assertMatch(pattern: Pattern, input: String) {
        assertTrue(matches(pattern, input))
    }

    /** Asserts that the given pattern does not match.  */
    private fun assertNoMatch(pattern: Pattern, input: String) {
        assertFalse(matches(pattern, input))
    }

    /** Returns true if the given pattern matches the input string.  */
    private fun matches(pattern: Pattern, input: String): Boolean {
        return pattern.matcher(input).matches()
    }

    /** Creates a case insensitive pattern.  */
    private fun getPattern(antPattern: String): Pattern {
        return AntPatternUtils.convertPattern(antPattern, false)
    }
}
