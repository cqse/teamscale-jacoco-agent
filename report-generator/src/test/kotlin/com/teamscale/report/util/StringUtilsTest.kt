/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright 2005-2011 The ConQAT Project                                   |
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

import org.conqat.lib.commons.string.StringUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for `StringUtils`
 */
class StringUtilsTest {

    /** Tests [StringUtils.editDistance].  */
    @Test
    fun testEditDistance() {
        assertEquals(0, StringUtils.editDistance("", ""))
        assertEquals(0, "abc".editDistance("abc"))

        assertEquals(1, StringUtils.editDistance("", "a"))
        assertEquals(1, "a".editDistance(""))
        assertEquals(1, "ab".editDistance("ac"))
        assertEquals(1, "abcdefgh".editDistance("abcefgh"))
        assertEquals(1, "abcefgh".editDistance("abcdefgh"))
        assertEquals(1, "abcdefgh".editDistance("abcXefgh"))

        assertEquals(2, "aaaaaa".editDistance("aaaa"))
        assertEquals(2, "aaaa".editDistance("aaaaaa"))

        assertEquals(2, "aaaaaa".editDistance("abaaba"))
        assertEquals(3, "The quick brown fox".editDistance("The _quick b_own fx"))

        assertEquals(13, "acb".editDistance("All different"))
    }
}
