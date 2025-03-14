package com.teamscale.report.compact

import org.assertj.core.api.Assertions.assertThat
import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.analysis.ILine
import org.jacoco.core.analysis.ISourceNode
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TeamscaleCompactCoverageBuilderTest {

	private val fullyCoveredLineInfo = mockLineInfo(ICounter.FULLY_COVERED)
	private val partlyCoveredLineInfo = mockLineInfo(ICounter.PARTLY_COVERED)
	private val noLineInfo = mockLineInfo(ICounter.EMPTY)

	@Test
	fun `buildReport should return empty coverage when no classes are visited`() {
		val builder = TeamscaleCompactCoverageBuilder()

		val report = builder.buildReport()

		assertThat(report.coverage).isEmpty()
		assertThat(report.version).isEqualTo(1)
	}

	@Test
	fun `buildReport should include coverage data for a single class`() {
		val builder = TeamscaleCompactCoverageBuilder()
		val classCoverage = mock(IClassCoverage::class.java).apply {
			`when`(sourceFileName).thenReturn("MyClass.java")
			`when`(packageName).thenReturn("com/example")
			`when`(firstLine).thenReturn(1)
			`when`(lastLine).thenReturn(3)
			`when`(getLine(1)).thenReturn(fullyCoveredLineInfo)
			`when`(getLine(2)).thenReturn(partlyCoveredLineInfo)
			`when`(getLine(3)).thenReturn(noLineInfo)
		}

		builder.visitCoverage(classCoverage)
		val report = builder.buildReport()

		assertThat(report.coverage).hasSize(1)
		assertThat(report.coverage[0].filePath).isEqualTo("com/example/MyClass.java")
		assertThat(report.coverage[0].fullyCoveredLines).containsExactly(1)
		assertThat(report.coverage[0].partiallyCoveredLines).containsExactly(2)
	}

	@Test
	fun `buildReport should merge coverage data for multiple classes in the same file`() {
		val builder = TeamscaleCompactCoverageBuilder()

		val firstCoverage = mock(IClassCoverage::class.java).apply {
			`when`(sourceFileName).thenReturn("SharedFile.java")
			`when`(packageName).thenReturn("com/example")
			`when`(firstLine).thenReturn(1)
			`when`(lastLine).thenReturn(2)
			`when`(getLine(1)).thenReturn(fullyCoveredLineInfo)
			`when`(getLine(2)).thenReturn(noLineInfo)
		}

		val secondCoverage = mock(IClassCoverage::class.java).apply {
			`when`(sourceFileName).thenReturn("SharedFile.java")
			`when`(packageName).thenReturn("com/example")
			`when`(firstLine).thenReturn(2)
			`when`(lastLine).thenReturn(3)
			`when`(getLine(2)).thenReturn(partlyCoveredLineInfo)
			`when`(getLine(3)).thenReturn(fullyCoveredLineInfo)
		}

		builder.visitCoverage(firstCoverage)
		builder.visitCoverage(secondCoverage)
		val report = builder.buildReport()

		assertThat(report.coverage).hasSize(1)
		assertThat(report.coverage[0].filePath).isEqualTo("com/example/SharedFile.java")
		assertThat(report.coverage[0].fullyCoveredLines).containsExactly(1, 3)
		assertThat(report.coverage[0].partiallyCoveredLines).containsExactly(2)
	}

	@Test
	fun `buildReport should handle classes with missing first or last line`() {
		val builder = TeamscaleCompactCoverageBuilder()

		val classCoverage = mock(IClassCoverage::class.java).apply {
			`when`(sourceFileName).thenReturn("InvalidClass.java")
			`when`(packageName).thenReturn("com/example")
			`when`(firstLine).thenReturn(ISourceNode.UNKNOWN_LINE)
			`when`(lastLine).thenReturn(ISourceNode.UNKNOWN_LINE)
		}

		builder.visitCoverage(classCoverage)
		val report = builder.buildReport()

		assertThat(report.coverage[0].fullyCoveredLines).isEmpty()
		assertThat(report.coverage[0].partiallyCoveredLines).isEmpty()
	}

	private fun mockLineInfo(status: Int): ILine {
		val line = mock(ILine::class.java)
		`when`(line.status).thenReturn(status)
		return line
	}
}
