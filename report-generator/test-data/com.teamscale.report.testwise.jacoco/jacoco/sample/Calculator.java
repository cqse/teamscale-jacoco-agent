/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 *
 */

package com.example.project;

public class Calculator {

	public int add(int a, int b) {
		return a + b;
	}

	public int mul(int a, int b) {
		int result = a;
		switch (a) {
			case 1:
				a = a + 1 - a;
				break;
			case 2:
				a = 2;
			case 3:
				a = 3; //bug
			default:

		}
		a *= 1;
		if (a==b) {
			return a*b;
		} else {
			int tmp = a;
			a = b;
			b = a;
		}
		result *= b;
		return result;
	}

	public int div(int a, int b) {
		return a / b;
	}
}
