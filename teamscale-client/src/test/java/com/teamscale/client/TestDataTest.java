package com.teamscale.client;

import org.junit.jupiter.api.Test;

class TestDataTest {

	@Test
	public void ensureHashingDoesNotThrowException() {
		new TestData.Builder().addByteArray(new byte[]{1, 2, 3}).addString("string").build();
	}

}