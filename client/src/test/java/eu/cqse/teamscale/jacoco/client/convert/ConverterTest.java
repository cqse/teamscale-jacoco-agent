package eu.cqse.teamscale.jacoco.client.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.test.CCSMTestCaseBase;
import org.junit.Test;

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
		arguments.inputFile = execFile.getAbsolutePath();
		arguments.outputFile = outputFile.getAbsolutePath();
		arguments.classDirectoriesOrZips = Arrays.asList(classFile.getAbsolutePath());

		new Converter(arguments).run();

		String xml = FileSystemUtils.readFileUTF8(outputFile);
		System.err.println(xml);
		assertThat(xml).isNotEmpty().contains("<class").contains("<counter").contains("TestClass");
	}

}
