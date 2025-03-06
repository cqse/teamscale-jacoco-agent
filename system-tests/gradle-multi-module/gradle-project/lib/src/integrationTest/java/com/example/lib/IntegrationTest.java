package com.example.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationTest {

	@Test
	public void myIntegrationTest() {
		Calculator cal = new Calculator();
		assertEquals(10, cal.add(cal.mul(cal.add(4, -1), 3), 1));
	}
}
