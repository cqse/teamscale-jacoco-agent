package com.teamscale.jacoco.agent.testimpact;

import com.google.gson.Gson;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Helper class for writing a list of test executions to a file. */
public class TestExecutionWriter {

	private static final Gson GSON = new Gson();

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
		FileWriter writer = new FileWriter(testExecutionFile);
		GSON.toJson(testExecutionsList, writer);
		writer.close();
	}
}
