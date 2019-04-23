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
package com.teamscale.client;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;

/**
 * A utility class providing some advanced string functionality.
 */
public class StringUtils {

	/** Line separator of the current platform. */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/** The empty string. */
	public static final String EMPTY_STRING = "";

	/** Number formatter. */
	private static NumberFormat numberFormat = NumberFormat.getInstance();

	/**
	 * Format number
	 */
	public static String format(Number number) {
		return numberFormat.format(number);
	}

	/**
	 * Checks if a string is empty (after trimming).
	 *
	 * @param text the string to check.
	 * @return <code>true</code> if string is empty or <code>null</code>,
	 * <code>false</code> otherwise.
	 */
	public static boolean isEmpty(String text) {
		if (text == null) {
			return true;
		}
		return EMPTY_STRING.equals(text.trim());
	}

	/**
	 * Determine if the supplied {@link String} is <em>blank</em> (i.e.,
	 * {@code null} or consisting only of whitespace characters).
	 *
	 * @param str the string to check; may be {@code null}
	 * @return {@code true} if the string is blank
	 */
	public static boolean isBlank(String str) {
		return (str == null || str.trim().isEmpty());
	}


	/**
	 * Returns the beginning of a String, cutting off the last part which is separated by the given character.
	 * <p>
	 * E.g., removeLastPart("org.conqat.lib.commons.string.StringUtils", '.') gives "org.conqat.lib.commons.string".
	 *
	 * @param string    the String
	 * @param separator separation character
	 * @return the String without the last part, or the original string if the separation character is not found.
	 */
	public static String removeLastPart(String string, char separator) {
		int idx = string.lastIndexOf(separator);
		if (idx == -1) {
			return string;
		}

		return string.substring(0, idx);
	}

	/**
	 * Remove suffix from a string.
	 *
	 * @param string the string
	 * @param suffix the suffix
	 * @return the string without the suffix or the original string if it does not end with the suffix.
	 */
	public static String stripSuffix(String string, String suffix) {
		if (string.endsWith(suffix)) {
			return string.substring(0, string.length() - suffix.length());
		}
		return string;
	}

	/**
	 * Create string representation of a map.
	 */
	public static String toString(Map<?, ?> map) {
		return toString(map, EMPTY_STRING);
	}

	/**
	 * Create string representation of a map.
	 *
	 * @param map    the map
	 * @param indent a line indent
	 */
	public static String toString(Map<?, ?> map, String indent) {
		StringBuilder result = new StringBuilder();
		Iterator<?> keyIterator = map.keySet().iterator();

		while (keyIterator.hasNext()) {
			result.append(indent);
			Object key = keyIterator.next();
			result.append(key);
			result.append(" = ");
			result.append(map.get(key));
			if (keyIterator.hasNext()) {
				result.append(LINE_SEPARATOR);
			}
		}

		return result.toString();
	}

	/**
	 * Format number with number formatter, if number formatter is
	 * <code>null</code>, this uses {@link String#valueOf(double)}.
	 */
	public static String format(double number, NumberFormat numberFormat) {
		if (numberFormat == null) {
			return String.valueOf(number);
		}
		return numberFormat.format(number);
	}

	/**
	 * Calculates the edit distance (aka Levenshtein distance) for two strings, i.e. the number of insert, delete or
	 * replace operations required to transform one string into the other. The running time is O(n*m) and the space
	 * complexity is O(n+m), where n/m are the lengths of the strings. Note that due to the high running time, for long
	 * strings the Diff class should be used, that has a more efficient algorithm, but only for insert/delete (not
	 * replace operation).
	 * <p>
	 * Although this is a clean reimplementation, the basic algorithm is explained here:
	 * http://en.wikipedia.org/wiki/Levenshtein_distance# Iterative_with_two_matrix_rows
	 */
	public static int editDistance(String s, String t) {
		char[] sChars = s.toCharArray();
		char[] tChars = t.toCharArray();
		int m = s.length();
		int n = t.length();

		int[] distance = new int[m + 1];
		for (int i = 0; i <= m; ++i) {
			distance[i] = i;
		}

		int[] oldDistance = new int[m + 1];
		for (int j = 1; j <= n; ++j) {

			// swap distance and oldDistance
			int[] tmp = oldDistance;
			oldDistance = distance;
			distance = tmp;

			distance[0] = j;
			for (int i = 1; i <= m; ++i) {
				int cost = 1 + Math.min(distance[i - 1], oldDistance[i]);
				if (sChars[i - 1] == tChars[j - 1]) {
					cost = Math.min(cost, oldDistance[i - 1]);
				} else {
					cost = Math.min(cost, 1 + oldDistance[i - 1]);
				}
				distance[i] = cost;
			}
		}

		return distance[m];
	}
}
