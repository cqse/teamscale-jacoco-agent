package com.teamscale.report.testwise.jacoco.cache

import com.teamscale.client.StringUtils
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder
import com.teamscale.report.util.CompactLines
import com.teamscale.report.util.ILogger
import org.jacoco.core.data.ExecutionData

/**
 * Holds information about a class' probes and to which line ranges they refer.
 *
 * Create an instance of this class for every analyzed java class.
 * Set the file name of the java source file from which the class has been created.
 * Then call [addProbe] for all probes and lines that belong to that probe.
 * Afterward call [getFileCoverage] to transform probes ([ExecutionData]) for this class into covered lines
 * ([FileCoverageBuilder]).
 *
 * @param className Classname as stored in the bytecode e.g., com/company/Example
 */
class ClassCoverageLookup internal constructor(
	private val className: String
) {
	var sourceFileName: String? = null
	private val probes = mutableMapOf<Int, CompactLines>()

	/** Adds the probe with the given id to the method. */
	fun addProbe(probeId: Int, lines: CompactLines) {
		probes[probeId] = lines
	}

	/**
	 * Generates [FileCoverageBuilder] from an [ExecutionData]. [ExecutionData] holds coverage of
	 * exactly one class (whereby inner classes are a separate class). This method returns a [FileCoverageBuilder]
	 * object which is later merged with the [FileCoverageBuilder] of other classes that reside in the same file.
	 */
	@Throws(CoverageGenerationException::class)
	fun getFileCoverage(executionData: ExecutionData, logger: ILogger): FileCoverageBuilder? {
		val executedProbes = executionData.probes

		when {
			probes.size > executedProbes.size -> throw CoverageGenerationException(
				"Probe lookup does not match with actual probe size for $sourceFileName $className (${probes.size} vs ${executedProbes.size})! This is a bug in the profiler tooling. Please report it back to CQSE."
			)

			sourceFileName == null -> {
				logger.warn("No source file name found for class $className! This class was probably not compiled with debug information enabled!")
				return null
			}
		}

		val packageName = if (className.contains("/")) StringUtils.removeLastPart(className, '/') else ""
		return FileCoverageBuilder(packageName, sourceFileName!!).apply {
			fillFileCoverage(this, executedProbes, logger)
		}
	}

	private fun fillFileCoverage(fileCoverage: FileCoverageBuilder, executedProbes: BooleanArray, logger: ILogger) {
		probes.forEach { (probeId, coveredLines) ->
			if (executedProbes.getOrNull(probeId) == true) {
				when {
					coveredLines.isEmpty -> logger.debug(
						"$sourceFileName $className contains a method with no line information. Does the class contain debug information?"
					)

					else -> fileCoverage.addLines(coveredLines)
				}
			} else {
				logger.info(
					"$sourceFileName $className contains a covered probe $probeId that could not be matched to any method. " +
							"This could be a bug in the profiler tooling. Please report it back to CQSE."
				)
			}
		}
	}
}
