package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.report.testwise.model.TestExecution;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Helper class for writing a list of test executions to a file. */
public class TestExecutionWriter {

	/** JSON adapter for test executions. */
	private JsonAdapter<List<TestExecution>> testExecutionsAdapter = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, TestExecution.class));

	private List<TestExecution> testExecutionsList = new ArrayList<>();

	private final File testExecutionFile;

	public TestExecutionWriter(File testExecutionFile) {
		this.testExecutionFile = testExecutionFile;
	}

	/** Appends the given {@link TestExecution} to the test execution list file. */
	public void append(TestExecution testExecution) throws IOException {
		testExecutionsList.add(testExecution);
		if (!testExecutionFile.exists()) {
			this.testExecutionFile.createNewFile();
		}
		try (BufferedSink writer = Okio.buffer(Okio.sink(testExecutionFile))) {
			testExecutionsAdapter.toJson(writer, testExecutionsList);
		}
	}
}
