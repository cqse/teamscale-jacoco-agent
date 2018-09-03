package eu.cqse.teamscale.report.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Wrapper around ConQAT ANT pattern utils to make it accessible from the other modules.
 */
public class AntPatternUtils {

	/** Converts an ANT pattern to a regex pattern. */
	public static Pattern convertPattern(String antPattern, boolean caseSensitive) throws PatternSyntaxException {
		return org.conqat.lib.commons.filesystem.AntPatternUtils.convertPattern(antPattern, caseSensitive);
	}
}