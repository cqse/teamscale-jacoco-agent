package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

	@Test
	void isBlank() {
		Map<String, Boolean> isEmptyByInput = new LinkedHashMap<>();
		isEmptyByInput.put("a", false);
		isEmptyByInput.put("\t", true);
		isEmptyByInput.put("\n", true);
		isEmptyByInput.put(" ", true);
		isEmptyByInput.put("  ", true);
		isEmptyByInput.put("\t  ", true);
		isEmptyByInput.put("\t  \n", true);

		isEmptyByInput.forEach((input, expected) -> assertSame(expected, StringUtils.isBlank(input)));
	}

	@Test
	void removeLastPart() {
		assertEquals("ab", StringUtils.removeLastPart("ab|cd", '|'));
	}

	@Test
	void stripPrefix() {
		assertEquals("|cd", StringUtils.stripPrefix("ab|cd", "ab"));
		assertEquals("ab|cd", StringUtils.stripPrefix("ab|cd", "b"));
	}

	@Test
	void stripSuffix() {
		assertEquals("ab|", StringUtils.stripSuffix("ab|cd", "cd"));
		assertEquals("ab|cd", StringUtils.stripSuffix("ab|cd", "c"));
	}

	@Test
	void testToString() {
		Map<String, Boolean> map = new LinkedHashMap<>();
		map.put("a", false);
		map.put("b", true);

		String string = StringUtils.toString(map);
		assertEquals("a = false\n" + "b = true", string);
	}

	@Test
	void format() {
		NumberFormat numberInstance = NumberFormat.getNumberInstance();
		numberInstance.setMaximumFractionDigits(2);
		numberInstance.setRoundingMode(RoundingMode.UP);
		assertEquals("12,35", StringUtils.format(12.3456, numberInstance));
	}

	@Test
	void editDistance() {
		assertSame(0, StringUtils.editDistance("a", "a"));
		assertSame(1, StringUtils.editDistance("ab", "a"));
		assertSame(2, StringUtils.editDistance("bb", "a"));
	}
}