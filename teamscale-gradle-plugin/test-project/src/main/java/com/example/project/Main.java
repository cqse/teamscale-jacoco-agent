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

public class Main {
	public static void main(String[] args) {
		Calculator cal = new Calculator();
		System.out.println("4/2+3 = " + cal.add(cal.div(4, 2), 3));
	}
}
