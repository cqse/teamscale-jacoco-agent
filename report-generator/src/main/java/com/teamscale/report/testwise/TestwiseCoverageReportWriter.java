package com.teamscale.report.testwise;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.factory.TestInfoFactory;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Writes out a {@link com.teamscale.report.testwise.model.TestwiseCoverageReport} one {@link TestInfo} after the other
 * so that we do not need to keep them all in memory during the conversion.
 */
public class TestwiseCoverageReportWriter implements Consumer<TestCoverageBuilder>, AutoCloseable {

	/** Factory for converting {@link TestCoverageBuilder} objects to {@link TestInfo}s. */
	private final TestInfoFactory testInfoFactory;

	/** Writer instance to where the {@link com.teamscale.report.testwise.model.TestwiseCoverageReport} is written to. */
	private final JsonWriter writer;

	/** Adapter instance for converting {@link TestInfo} objects to JSON. */
	private final JsonAdapter<TestInfo> testInfoJsonAdapter;

	public TestwiseCoverageReportWriter(TestInfoFactory testInfoFactory, File outputFile) throws IOException {
		this.testInfoFactory = testInfoFactory;
		testInfoJsonAdapter = new Moshi.Builder().build().adapter(TestInfo.class).indent("\t");

		writer = JsonWriter.of(Okio.buffer(Okio.sink(outputFile)));
		writer.beginObject();
		writer.name("tests");
		writer.beginArray();
	}

	@Override
	public void accept(TestCoverageBuilder testCoverageBuilder) {
		TestInfo testInfo = testInfoFactory.createFor(testCoverageBuilder);
		try {
			testInfoJsonAdapter.toJson(writer, testInfo);
		} catch (IOException e) {
			throw new RuntimeException("Writing test info to report failed.", e);
		}
	}

	@Override
	public void close() throws IOException {
		for (TestInfo testInfo : testInfoFactory.createTestInfosWithoutCoverage()) {
			testInfoJsonAdapter.toJson(writer, testInfo);
		}
		writer.endArray();
		writer.endObject();
		writer.close();
	}
}
