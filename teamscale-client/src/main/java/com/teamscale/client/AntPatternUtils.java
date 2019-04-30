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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility methods for dealing with Ant pattern as defined at http://ant.apache.org/manual/dirtasks.html#patterns
 * <p>
 * We implement a special version where a trailing '.' can be used to only match files without file extension (i.e. file
 * names without dot).
 */
public class AntPatternUtils {

	/** Converts an ANT pattern to a regex pattern. */
	public static Pattern convertPattern(String antPattern, boolean caseSensitive) throws PatternSyntaxException {

		antPattern = normalizePattern(antPattern);

		// ant specialty: trailing /** is optional
		// for example **/e*/** will also match foo/entry
		boolean addTrailAll = false;
		if (antPattern.endsWith("/**")) {
			addTrailAll = true;
			antPattern = StringUtils.stripSuffix(antPattern, "/**");
		}

		StringBuilder patternBuilder = new StringBuilder();
		convertPlainPattern(antPattern, patternBuilder);

		if (addTrailAll) {
			// the tail pattern is optional (i.e. we do not require the '/'),
			// but the "**" is only in effect if the '/' occurs
			patternBuilder.append("(/.*)?");
		}

		return compileRegex(patternBuilder.toString(), antPattern, caseSensitive);
	}

	/** Compiles the given regex. */
	private static Pattern compileRegex(String regex, String antPattern, boolean caseSensitive) {
		try {
			return Pattern.compile(regex, determineRegexFlags(caseSensitive));
		} catch (PatternSyntaxException e) {
			// make pattern syntax exception more understandable
			throw new PatternSyntaxException(
					"Error compiling ANT pattern '" + antPattern + "' to regular expression. " + e.getDescription(),
					e.getPattern(), e.getIndex());
		}
	}

	/** Returns the flags to be used for the regular expression. */
	private static int determineRegexFlags(boolean caseSensitive) {
		// Use DOTALL flag, as on Unix the file names can contain line breaks
		int flags = Pattern.DOTALL;
		if (!caseSensitive) {
			flags |= Pattern.CASE_INSENSITIVE;
		}
		return flags;
	}

	/**
	 * Normalizes the given pattern by ensuring forward slashes and mapping trailing slash to '/**'.
	 */
	private static String normalizePattern(String antPattern) {
		antPattern = FileSystemUtils.normalizeSeparators(antPattern);

		// ant pattern syntax: if a pattern ends with /, then ** is
		// appended
		if (antPattern.endsWith("/")) {
			antPattern += "**";
		}
		return antPattern;
	}

	/**
	 * Converts a plain ANT pattern to a regular expression, by replacing special characters, such as '?', '*', and
	 * '**'. The created pattern is appended to the given {@link StringBuilder}. The pattern must be plain, i.e. all ANT
	 * specialties, such as trailing double stars have to be dealt with beforehand.
	 */
	private static void convertPlainPattern(String antPattern, StringBuilder patternBuilder) {
		for (int i = 0; i < antPattern.length(); ++i) {
			char c = antPattern.charAt(i);
			if (c == '?') {
				patternBuilder.append("[^/]");
			} else if (c != '*') {
				patternBuilder.append(Pattern.quote(Character.toString(c)));
			} else {
				i = convertStarSequence(antPattern, patternBuilder, i);
			}
		}
	}

	/**
	 * Converts a sequence of the ant pattern starting with a star at the given index. Appends the pattern fragment the
	 * the builder and returns the index to continue scanning from.
	 */
	private static int convertStarSequence(String antPattern, StringBuilder patternBuilder, int index) {
		boolean doubleStar = isCharAt(antPattern, index + 1, '*');
		if (doubleStar) {
			// if the double star is followed by a slash, the entire
			// group becomes optional, as we want "**/foo" to also
			// match a top-level "foo"
			boolean doubleStarSlash = isCharAt(antPattern, index + 2, '/');
			if (doubleStarSlash) {
				patternBuilder.append("(.*/)?");
				return index + 2;
			}

			boolean doubleStarDot = isCharAtBeforeSlashOrEnd(antPattern, index + 2, '.');
			if (doubleStarDot) {
				patternBuilder.append("(.*/)?[^/.]*[.]?");
				return index + 2;
			}

			patternBuilder.append(".*");
			return index + 1;
		}

		boolean starDot = isCharAtBeforeSlashOrEnd(antPattern, index + 1, '.');
		if (starDot) {
			patternBuilder.append("[^/.]*[.]?");
			return index + 1;
		}

		patternBuilder.append("[^/]*");
		return index;
	}

	/**
	 * Returns whether the given position exists in the string and equals the given character, and the given character
	 * is either at the end or right before a slash.
	 */
	private static boolean isCharAtBeforeSlashOrEnd(String s, int position, char character) {
		return isCharAt(s, position, character) && (position + 1 == s.length() || isCharAt(s, position + 1, '/'));
	}

	/**
	 * Returns whether the given position exists in the string and equals the given character.
	 */
	private static boolean isCharAt(String s, int position, char character) {
		return position < s.length() && s.charAt(position) == character;
	}
}