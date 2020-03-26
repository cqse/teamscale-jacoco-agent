package com.teamscale.jacoco.agent.testimpact;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestExecutionWriterTest {

	@Test
	public void testOneExecution() throws Exception {
		Path tempFile = Files.createTempFile("TestExecutionWriterTest", "tmp");
		try {
			TestExecutionWriter writer = new TestExecutionWriter(tempFile.toFile());
			writer.append(new TestExecution("test1", 123, ETestExecutionResult.PASSED));
			String json = String.join("\n", Files.readAllLines(tempFile));
			assertThat(json).isEqualTo("[{\"durationMillis\":123,\"result\":\"PASSED\",\"uniformPath\":\"test1\"}]");
		} finally {
			tempFile.toFile().delete();
		}
	}

	@Test
	public void testMultipleExecutions() throws Exception {
		Path tempFile = Files.createTempFile("TestExecutionWriterTest", "tmp");
		try {
			TestExecutionWriter writer = new TestExecutionWriter(tempFile.toFile());
			writer.append(new TestExecution("test1", 123, ETestExecutionResult.PASSED));
			writer.append(new TestExecution("test2", 123, ETestExecutionResult.PASSED));
			writer.append(new TestExecution("test3", 123, ETestExecutionResult.PASSED));
			String json = String.join("\n", Files.readAllLines(tempFile));
			assertThat(json).isEqualTo("[{\"durationMillis\":123,\"result\":\"PASSED\",\"uniformPath\":\"test1\"}" +
					",{\"durationMillis\":123,\"result\":\"PASSED\",\"uniformPath\":\"test2\"}" +
					",{\"durationMillis\":123,\"result\":\"PASSED\",\"uniformPath\":\"test3\"}]");
		} finally {
			tempFile.toFile().delete();
		}
	}

}