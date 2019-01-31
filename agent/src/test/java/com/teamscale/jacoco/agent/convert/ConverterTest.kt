package com.teamscale.jacoco.agent.convert;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/** Basic smoke test for the converter. */
public class ConverterTest extends CCSMTestCaseBase {

	/**
	 * Ensures that running the converter on valid input does not yield any errors
	 * and produces a coverage XML report.
	 */
	@Test
	public void testSmokeTest() throws Exception {
		File execFile = useTestFile("coverage.exec");
		File classFile = useTestFile("TestClass.class");
		File outputFile = createTmpFile("coverage.xml", "");

		ConvertCommand arguments = new ConvertCommand();
		arguments.setInputFiles(Collections.singletonList(execFile.getAbsolutePath()));
		arguments.setOutputFile(outputFile.getAbsolutePath());
		arguments.setClassDirectoriesOrZips(Collections.singletonList(classFile.getAbsolutePath()));

		new Converter(arguments).runJaCoCoReportGeneration();

		String xml = FileSystemUtils.readFileUTF8(outputFile);
		System.err.println(xml);
		assertThat(xml).isNotEmpty().contains("<class").contains("<counter").contains("TestClass");
	}

}
