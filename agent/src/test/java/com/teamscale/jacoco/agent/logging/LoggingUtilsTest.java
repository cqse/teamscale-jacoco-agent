package com.teamscale.jacoco.agent.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingUtilsTest {

	@Test
	void testGetStackTraceAsString() {
		Exception causeEx = new Exception("Test cause exception");
		Exception exception = new Exception("Test exception", causeEx);
		String stackTrace = LoggingUtils.getStackTraceAsString(exception);

		assertThat(stackTrace).contains("Test cause exception").contains("Test exception").contains("at ");
	}
}
