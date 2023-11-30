package com.teamscale.report.testwise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.teamscale.client.StringUtils;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.factory.TestInfoFactory;
import com.teamscale.report.util.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Writes out a {@link com.teamscale.report.testwise.model.TestwiseCoverageReport} one {@link TestInfo} after the other
 * so that we do not need to keep them all in memory during the conversion.
 */
public class TestwiseCoverageReportWriter implements Consumer<TestCoverageBuilder>, AutoCloseable {

	/** Factory for converting {@link TestCoverageBuilder} objects to {@link TestInfo}s. */
	private final TestInfoFactory testInfoFactory;

	private final File outputFile;
	/** After how many written tests a new file should be started. */
	private final int splitAfter;

	/** Writer instance to where the {@link com.teamscale.report.testwise.model.TestwiseCoverageReport} is written to. */
	private JsonGenerator jsonGenerator;

	/** Number of tests written to the file. */
	private int testsWritten = 0;

	/** Number of test files that have been written. */
	private int testFileCounter = 0;

	public TestwiseCoverageReportWriter(TestInfoFactory testInfoFactory, File outputFile,
										int splitAfter) throws IOException {
		this.testInfoFactory = testInfoFactory;
		this.outputFile = outputFile;
		this.splitAfter = splitAfter;

		startReport();
	}

	@Override
	public void accept(TestCoverageBuilder testCoverageBuilder) {
		TestInfo testInfo = testInfoFactory.createFor(testCoverageBuilder);
		try {
			writeTestInfo(testInfo);
		} catch (IOException e) {
			// Need to be wrapped in RuntimeException as Consumer does not allow to throw a checked Exception
			throw new RuntimeException("Writing test info to report failed.", e);
		}
	}

	@Override
	public void close() throws IOException {
		for (TestInfo testInfo : testInfoFactory.createTestInfosWithoutCoverage()) {
			writeTestInfo(testInfo);
		}
		endReport();
	}

	private void startReport() throws IOException {
		testFileCounter++;
		OutputStream outputStream = Files.newOutputStream(getOutputFile(testFileCounter).toPath());
		jsonGenerator = JsonUtils.createFactory().createGenerator(outputStream);
		jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
		jsonGenerator.writeStartObject();
		jsonGenerator.writeFieldName("tests");
		jsonGenerator.writeStartArray();
	}

	private File getOutputFile(int testFileCounter) {
		String name = this.outputFile.getName();
		name = StringUtils.stripSuffix(name, ".json");
		name = name + "-" + testFileCounter + ".json";
		return new File(this.outputFile.getParent(), name);
	}

	private void writeTestInfo(TestInfo testInfo) throws IOException {
		if (testsWritten >= splitAfter) {
			endReport();
			testsWritten = 0;
			startReport();
		}
		jsonGenerator.writeObject(testInfo);
		testsWritten++;
	}

	private void endReport() throws IOException {
		jsonGenerator.writeEndArray();
		jsonGenerator.writeEndObject();
		jsonGenerator.close();
	}
}
