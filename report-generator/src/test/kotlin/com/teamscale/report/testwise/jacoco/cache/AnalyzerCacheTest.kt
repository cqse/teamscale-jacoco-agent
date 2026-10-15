package com.teamscale.report.testwise.jacoco.cache

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.assertj.core.api.Assertions.assertThat
import org.jacoco.core.internal.data.CRC64
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Tests for the [AnalyzerCache] class.  */
class AnalyzerCacheTest {

	/**
	 * A JAR may contain other JARs (e.g. SVNKit). The analyzer addresses a nested JAR as `outer.jar@path/inner.jar`,
	 * which is not a file on disk, so the cache must not try to read it from there.
	 */
	@Test
	fun analyzesClassesInNestedJars(@TempDir tempDir: File) {
		val classBytes = javaClass.getResourceAsStream("AnalyzerCacheTest.class")!!.use { it.readBytes() }
		val innerJar = zip("com/teamscale/report/testwise/jacoco/cache/AnalyzerCacheTest.class" to classBytes)
		tempDir.resolve("outer.jar").writeBytes(zip("lib/inner.jar" to innerJar))

		val logger = Mockito.mock<ILogger>()
		val probesCache = ProbesCache(logger, EDuplicateClassFileBehavior.IGNORE)
		val count = AnalyzerCache(probesCache, ClasspathWildcardIncludeFilter(null, null), logger)
			.analyzeAll(tempDir)

		assertThat(count).isEqualTo(1)
		assertThat(probesCache.containsClassId(CRC64.classId(classBytes))).isTrue()
	}

	private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
		val output = ByteArrayOutputStream()
		ZipOutputStream(output).use { zip ->
			entries.forEach { (name, content) ->
				zip.putNextEntry(ZipEntry(name))
				zip.write(content)
				zip.closeEntry()
			}
		}
		return output.toByteArray()
	}
}
