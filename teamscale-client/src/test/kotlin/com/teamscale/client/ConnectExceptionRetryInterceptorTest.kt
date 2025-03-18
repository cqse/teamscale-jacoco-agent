package com.teamscale.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.math.pow

class ConnectExceptionRetryInterceptorTest {

	@Test
	fun `should return value within bounds for first retry`() {
		val interceptor = ConnectExceptionRetryInterceptor(timeout = Duration.ofSeconds(30))
		val attemptCount = 1

		val delay = interceptor.calculateBackoffDelay(attemptCount)

		assertThat(delay).isBetween(400L, 600L)
	}

	@Test
	fun `should increase delay for second retry`() {
		val interceptor = ConnectExceptionRetryInterceptor(timeout = Duration.ofSeconds(30))
		val attemptCount = 2

		val delay = interceptor.calculateBackoffDelay(attemptCount)

		val expectedBaseDelay = 500 * 2.0.pow(attemptCount - 1).toLong()
		assertThat(delay).isBetween((expectedBaseDelay * 0.8).toLong(), (expectedBaseDelay * 1.2).toLong())
	}

	@Test
	fun `should cap delay at max limit on high retry count`() {
		val interceptor = ConnectExceptionRetryInterceptor(timeout = Duration.ofSeconds(30))
		val attemptCount = 10

		val delay = interceptor.calculateBackoffDelay(attemptCount)

		assertThat(delay).isLessThanOrEqualTo(5000L)
	}
}
