package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for writing a list of test executions to a file. This class ensures that we never have to hold all test
 * executions in memory but rather incrementally append to the output file. This ensures that we don't use unnecessary
 * amounts of memory during profiling.
 */
public class TestExecutionWriter {

	private final JsonAdapter<TestExecution> testExecutionAdapter = new Moshi.Builder().build()
			.adapter(TestExecution.class);

	private final File testExecutionFile;
	private boolean hasWrittenAtLeastOneExecution = false;

	public TestExecutionWriter(File testExecutionFile) {
		this.testExecutionFile = testExecutionFile;
	}

	/** Appends the given {@link TestExecution} to the test execution list file. */
	public synchronized void append(TestExecution testExecution) throws IOException {
		String json = testExecutionAdapter.toJson(testExecution);

		// the file contains a JSON array if it exists and to append to it, we strip the trailing "]" and append
		// our new entry and a closing "]"
		// "rwd" means open for read-write and flush all changes directly to disk
		try (RandomAccessFile file = new RandomAccessFile(testExecutionFile, "rwd")) {
			String textToWrite = json + "]";
			if (hasWrittenAtLeastOneExecution) {
				textToWrite = "," + textToWrite;
				// overwrite the trailing "]"
				file.seek(file.length() - 1);
			} else {
				textToWrite = "[" + textToWrite;
			}

			file.write(textToWrite.getBytes(StandardCharsets.UTF_8));
		}

		hasWrittenAtLeastOneExecution = true;
	}

}
