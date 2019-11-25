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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

class FailingRepeatedTest {

	public static int i=5;

	@Test
	public void testAdd() {
		assertEquals(3, new Calculator().add(1, 2));
	}

	@RepeatedTest(2)
	public void testRepeatedTest() {
		assertEquals(i++, new Calculator().mul(2, 3));
	}

}
