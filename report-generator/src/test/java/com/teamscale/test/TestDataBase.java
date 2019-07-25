package com.teamscale.test;

import java.io.File;

/** Base class that supports reading test-data files. */
public class TestDataBase {

	/** Read the given test-data file in the context of the current class's package. */
	protected File useTestFile(String fileName) {
		return new File(new File("test-data", getClass().getPackage().getName()), fileName);
	}
}
