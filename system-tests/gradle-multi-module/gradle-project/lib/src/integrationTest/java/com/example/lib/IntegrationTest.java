package com.example.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class IntegrationTest {

	@Test
	public void myIntegrationTest() {
		Calculator cal = new Calculator();
		assertEquals(10, cal.add(cal.mul(cal.add(4, -1), 3), 1));
	}
}
