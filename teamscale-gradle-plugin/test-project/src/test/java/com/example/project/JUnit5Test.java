/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.example.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JUnit5Test {

	@Test
	public void testAdd() {
		assertEquals(3, new Calculator().add(1, 2));
	}

	@Test
	@Tag("integration")
	public void systemTest() {
		Calculator cal = new Calculator();
		assertEquals(10, cal.add(cal.mul(cal.add(4, -1), 3), 1));
	}

	@Test
	@Tag("integration")
	public void testMain() {
		Main.main(new String[0]);
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "Hello", "JUnit" })
	void withValueSource(String word) {
		assertNotNull(word);
	}
}
