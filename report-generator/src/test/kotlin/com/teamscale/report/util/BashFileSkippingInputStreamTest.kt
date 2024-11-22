package com.teamscale.report.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

internal class BashFileSkippingInputStreamTest {
	@Test
	@Throws(IOException::class)
	fun testBashFileJar() {
		val filesInJar = getEntriesFromJarFile("spring-boot-executable-example.jar")
		Assertions.assertThat(filesInJar).hasSize(110)
	}

	@Test
	@Throws(IOException::class)
	fun testNormalJar() {
		val filesInJar = getEntriesFromJarFile("normal.jar")
		Assertions.assertThat(filesInJar).hasSize(284)
	}

	@Throws(IOException::class)
	private fun getEntriesFromJarFile(resourceName: String): List<String> {
		val inputStream = javaClass.getResourceAsStream(resourceName)
		val bashFileSkippingInputStream = BashFileSkippingInputStream(inputStream!!)
		val jarInputStream = JarInputStream(bashFileSkippingInputStream)
		return generateSequence { jarInputStream.nextJarEntry }
			.map { it.name }
			.toList()
	}
}
