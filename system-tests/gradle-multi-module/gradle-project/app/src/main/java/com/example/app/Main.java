package com.example.app;

import com.example.lib.Calculator;

public class Main {
	public static void main(String[] args) {
		Calculator cal = new Calculator();
		System.out.println("4/2+3 = " + cal.add(cal.div(4, 2), 3));
	}
}
