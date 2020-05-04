package com.teamscale.jacoco.agent.convert;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/** Basic smoke test for the converter. */
public class ConverterTest {

	/**
	 * Ensures that running the converter on valid input does not yield any errors and produces a coverage XML report.
	 */
	@Test
	public void testSmokeTest(@TempDir File tempDir) throws Exception {
		File execFile = new File(getClass().getResource("coverage.exec").toURI());
		File classFile = new File(getClass().getResource("TestClass.class").toURI());
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

	/**
	 * Ensures that running the converter on valid input does not yield any errors and produces a coverage XML report.
	 */
	@Test
	public void testTestwiseCoverageSmokeTest(@TempDir File tempDir) throws Exception {
		File inputDir = new File(tempDir, "input");
		inputDir.mkdir();
		copyResourceTo(inputDir, "coverage-testwise.exec");
		copyResourceTo(inputDir, "test-list.json");
		File classFile = new File(getClass().getResource("classes.zip").toURI());
		File outputFile = new File(tempDir, "testwise-coverage.json");

		ConvertCommand arguments = new ConvertCommand();
		arguments.inputFiles = Collections.singletonList(inputDir.getAbsolutePath());
		arguments.outputFile = outputFile.getAbsolutePath();
		arguments.classDirectoriesOrZips = Collections.singletonList(classFile.getAbsolutePath());

		new Converter(arguments).runTestwiseCoverageReportGeneration();

		String json = FileSystemUtils.readFileUTF8(new File(tempDir, "testwise-coverage-1.json"));
		System.err.println(json);
		assertThat(json).
				contains(
						"\"uniformPath\": \"[engine:junit-vintage]/[runner:org.conqat.lib.cqddl.CQDDLTest]/[test:testFunctions(org.conqat.lib.cqddl.CQDDLTest)]\"")
				.contains(
						"\"uniformPath\": \"[engine:junit-vintage]/[runner:org.conqat.lib.cqddl.CQDDLTest]/[test:testDirectObjectInsertion(org.conqat.lib.cqddl.CQDDLTest)]\"")
				.contains("\"uniformPath\": \"[engine:junit-vintage]/[runner:org.conqat.lib.cqddl.CQDDLTest]/[test:testKeyAbbreviations(org.conqat.lib.cqddl.CQDDLTest)]\"")
				.contains("\"uniformPath\": \"[engine:junit-vintage]/[runner:org.conqat.lib.cqddl.CQDDLTest]/[test:testKeyAbbreviations(org.conqat.lib.cqddl.CQDDLTest)]\"")
		.contains("\"result\": \"PASSED\"").contains("\"duration\": 1234").contains("\"coveredLines\": \"33,46-47");
	}

	public void copyResourceTo(File inputDir, String name) throws URISyntaxException, IOException {
		File execFile = new File(getClass().getResource(name).toURI());
		Files.copy(execFile.toPath(), new File(inputDir, name).toPath());
	}

}
