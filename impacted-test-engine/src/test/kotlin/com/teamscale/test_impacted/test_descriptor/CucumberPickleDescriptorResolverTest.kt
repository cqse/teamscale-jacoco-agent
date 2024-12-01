package com.teamscale.test_impacted.test_descriptor

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CucumberPickleDescriptorResolverTest {
	@Test
	fun escapeSlashes() {
		mapOf(
			"abc" to "abc",
			"ab/c" to "ab\\/c",
			"ab\\/c" to "ab\\/c", // don't escape what is already escaped
			"/abc" to "\\/abc",
			"/abc/" to "\\/abc\\/",
			"/" to "\\/",
			"/a/" to "\\/a\\/",
			"a//" to "a\\/\\/",
			"//a" to "\\/\\/a",
			"//" to "\\/\\/",
			"///" to "\\/\\/\\/",
			"\\" to "\\",
			"http://link" to "http:\\/\\/link"
		).forEach { (input, expected) ->
			Assertions.assertEquals(expected, CucumberPickleDescriptorResolver.escapeSlashes(input))
		}
	}

	@Test
	fun testNoDuplicatedSlashesInUniformPath() {
		mapOf(
			"abc" to "abc",
			"ab/c" to "ab/c",
			"ab//c" to "ab/c",
			"ab///c" to "ab/c",
			"ab\\/\\//c" to "ab\\/\\//c",
			"a/" to "a/",
			"a//" to "a/",
			"/a" to "/a",
			"//a" to "/a",
			"/" to "/",
			"\\/" to "\\/",
			"\\" to "\\",
			"\\\\" to "\\\\"
		).forEach { (input, expected) ->
			Assertions.assertEquals(expected, CucumberPickleDescriptorResolver().removeDuplicatedSlashes(input))
		}
	}
}