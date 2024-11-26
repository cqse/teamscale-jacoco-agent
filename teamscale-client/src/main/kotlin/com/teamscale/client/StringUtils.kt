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
package com.teamscale.client

import java.text.NumberFormat
import kotlin.math.min

/**
 * A utility class providing some advanced string functionality.
 */
object StringUtils {
	/** Line separator of the current platform.  */
	private val LINE_SEPARATOR: String = System.lineSeparator()

	/** The empty string.  */
	private const val EMPTY_STRING: String = ""

	/**
	 * Checks if a string is empty (after trimming).
	 *
	 * @param text the string to check.
	 * @return `true` if string is empty or `null`,
	 * `false` otherwise.
	 */
	@JvmStatic
	fun isEmpty(text: String?): Boolean {
		if (text == null) {
			return true
		}
		return EMPTY_STRING == text.trim { it <= ' ' }
	}

	/**
	 * Determine if the supplied [String] is *blank* (i.e., `null` or consisting only of whitespace
	 * characters).
	 *
	 * @param str the string to check; may be `null`
	 * @return `true` if the string is blank
	 */
	@JvmStatic
	fun isBlank(str: String?): Boolean {
		return (str == null || str.trim { it <= ' ' }.isEmpty())
	}


	/**
	 * Returns the beginning of a String, cutting off the last part which is separated by the given character.
	 *
	 *
	 * E.g., removeLastPart("org.conqat.lib.commons.string.StringUtils", '.') gives "org.conqat.lib.commons.string".
	 *
	 * @param string    the String
	 * @param separator separation character
	 * @return the String without the last part, or the original string if the separation character is not found.
	 */
	fun removeLastPart(string: String, separator: Char): String {
		val idx = string.lastIndexOf(separator)
		if (idx == -1) {
			return string
		}

		return string.substring(0, idx)
	}

	/**
	 * Remove prefix from a string.
	 *
	 * @param string the string
	 * @param prefix the prefix
	 * @return the string without the prefix or the original string if it does not start with the prefix.
	 */
	@JvmStatic
	fun stripPrefix(string: String, prefix: String): String {
		if (string.startsWith(prefix)) {
			return string.substring(prefix.length)
		}
		return string
	}

	/**
	 * Remove suffix from a string.
	 *
	 * @param string the string
	 * @param suffix the suffix
	 * @return the string without the suffix or the original string if it does not end with the suffix.
	 */
	@JvmStatic
	fun stripSuffix(string: String, suffix: String): String {
		if (string.endsWith(suffix)) {
			return string.substring(0, string.length - suffix.length)
		}
		return string
	}

	/**
	 * Create string representation of a map.
	 *
	 * @param map    the map
	 * @param indent a line indent
	 */
	/**
	 * Create string representation of a map.
	 */
	@JvmOverloads
	fun toString(map: Map<*, *>, indent: String? = EMPTY_STRING): String {
		val result = StringBuilder()
		val keyIterator = map.keys.iterator()

		while (keyIterator.hasNext()) {
			result.append(indent)
			val key = keyIterator.next()!!
			result.append(key)
			result.append(" = ")
			result.append(map[key])
			if (keyIterator.hasNext()) {
				result.append(LINE_SEPARATOR)
			}
		}

		return result.toString()
	}

	/**
	 * Format number with number formatter, if number formatter is
	 * `null`, this uses [String.valueOf].
	 */
	fun format(number: Double, numberFormat: NumberFormat?): String {
		if (numberFormat == null) {
			return number.toString()
		}
		return numberFormat.format(number)
	}

	/**
	 * Calculates the edit distance (aka Levenshtein distance) for two strings, i.e. the number of insert, delete or
	 * replace operations required to transform one string into the other. The running time is O(n*m) and the space
	 * complexity is O(n+m), where n/m are the lengths of the strings. Note that due to the high running time, for long
	 * strings the Diff class should be used, that has a more efficient algorithm, but only for insert/delete (not
	 * replace operation).
	 *
	 *
	 * Although this is a clean reimplementation, the basic algorithm is explained here:
	 * http://en.wikipedia.org/wiki/Levenshtein_distance# Iterative_with_two_matrix_rows
	 */
	@JvmStatic
	fun editDistance(s: String, t: String): Int {
		val sChars = s.toCharArray()
		val tChars = t.toCharArray()
		val m = s.length
		val n = t.length

		var distance = IntArray(m + 1)
		for (i in 0..m) {
			distance[i] = i
		}

		var oldDistance = IntArray(m + 1)
		for (j in 1..n) {
			// swap distance and oldDistance

			val tmp = oldDistance
			oldDistance = distance
			distance = tmp

			distance[0] = j
			for (i in 1..m) {
				var cost = (1 + min(
					distance[i - 1].toDouble(),
					oldDistance[i].toDouble()
				)).toInt()
				cost = if (sChars[i - 1] == tChars[j - 1]) {
					min(cost.toDouble(), oldDistance[i - 1].toDouble()).toInt()
				} else {
					min(cost.toDouble(), (1 + oldDistance[i - 1]).toDouble()).toInt()
				}
				distance[i] = cost
			}
		}

		return distance[m]
	}
}
