package com.teamscale.jacoco.agent.convert;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.ManagedTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/** Basic smoke test for the converter. */
public class ConverterTest {

	private ManagedTestData managedTestData = new ManagedTestData(getClass());

	/**
	 * Ensures that running the converter on valid input does not yield any errors and produces a coverage XML report.
	 */
	@Test
	public void testSmokeTest(@TempDir File tempDir) throws Exception {
		File execFile = managedTestData.useDataFile("coverage.exec");
		File classFile = managedTestData.useDataFile("TestClass.class");
		File outputFile = new File(tempDir, "coverage.xml");

		ConvertCommand arguments = new ConvertCommand();
		arguments.inputFiles = Collections.singletonList(execFile.getAbsolutePath());
		arguments.outputFile = outputFile.getAbsolutePath();
		arguments.classDirectoriesOrZips = Collections.singletonList(classFile.getAbsolutePath());

		new Converter(arguments).runJaCoCoReportGeneration();

		String xml = FileSystemUtils.readFileUTF8(outputFile);
		System.err.println(xml);
		assertThat(xml).isNotEmpty().contains("<class").contains("<counter").contains("TestClass");
	}

}
