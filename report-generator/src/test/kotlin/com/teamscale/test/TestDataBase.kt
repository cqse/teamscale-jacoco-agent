package com.teamscale.test

import java.io.File

/** Base class that supports reading test-data files.  */
open class TestDataBase {
	/** Read the given test-data file in the context of the current class's package.  */
	protected fun useTestFile(fileName: String) =
		File(File("test-data", javaClass.getPackage().name), fileName)
}
