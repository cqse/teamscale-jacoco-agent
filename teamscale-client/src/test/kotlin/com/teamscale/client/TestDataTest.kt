package com.teamscale.client

import org.junit.jupiter.api.Test

internal class TestDataTest {
	@Test
	fun ensureHashingDoesNotThrowException() {
		TestData.Builder().addByteArray(byteArrayOf(1, 2, 3)).addString("string").build()
	}
}