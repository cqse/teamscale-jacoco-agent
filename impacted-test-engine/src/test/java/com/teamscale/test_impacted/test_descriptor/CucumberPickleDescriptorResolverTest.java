package com.teamscale.test_impacted.test_descriptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

class CucumberPickleDescriptorResolverTest {

	@Test
	void escapeSlashes() {
		LinkedHashMap<String, String> expectedByInput = new LinkedHashMap<>();
		expectedByInput.put("abc", "abc");
		expectedByInput.put("ab/c", "ab\\/c");
		expectedByInput.put("ab\\/c", "ab\\/c"); // don't escape what is already escaped
		expectedByInput.put("/abc", "\\/abc");
		expectedByInput.put("/abc/", "\\/abc\\/");
		expectedByInput.put("/", "\\/");
		expectedByInput.put("/a/", "\\/a\\/");
		expectedByInput.put("a//", "a\\/\\/");
		expectedByInput.put("//a", "\\/\\/a");
		expectedByInput.put("//", "\\/\\/");
		expectedByInput.put("\\", "\\");
		expectedByInput.put("http://link", "http:\\/\\/link");

		expectedByInput.forEach((input, expected) -> Assertions.assertEquals(expected,
				CucumberPickleDescriptorResolver.escapeSlashes(input)));
	}
}