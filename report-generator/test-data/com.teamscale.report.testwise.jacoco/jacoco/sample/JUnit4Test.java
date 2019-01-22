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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Ignore;

public class JUnit4Test {

	@Test
	public void testMul() {
		assertEquals(9, new Calculator().mul(2, 3));
	}

	@Test
	public void testMul2() {
		assertEquals(2, new Calculator().mul(1, 2));
	}
}
