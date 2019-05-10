package com.teamscale.jacoco.agent.testimpact;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/** Helper class for writing a list of test executions to a file. */
public class TestExecutionWriter {

	private static final Gson GSON = new Gson();

	private final File testExecutionFile;

	public TestExecutionWriter(File testExecutionFile) {
		this.testExecutionFile = testExecutionFile;
	}

	/** Appends the given {@link TestExecution} to the test execution list file. */
	public void append(TestExecution testExecution) throws IOException {
		if (!testExecutionFile.exists()) {
			this.testExecutionFile.createNewFile();
		}
		FileReader fileReader = new FileReader(testExecutionFile);
		List<TestExecution> testExecutions = GSON
				.fromJson(fileReader, new TypeToken<List<TestExecution>>() {
				}.getType());
		fileReader.close();
		testExecutions.add(testExecution);
		FileWriter writer = new FileWriter(testExecutionFile);
		GSON.toJson(testExecutions, writer);
		writer.close();
	}
}
