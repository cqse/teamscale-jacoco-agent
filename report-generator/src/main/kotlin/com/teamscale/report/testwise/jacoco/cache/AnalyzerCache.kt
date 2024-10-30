package com.teamscale.report.testwise.jacoco.cache

import com.teamscale.report.jacoco.FilteringAnalyzer
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.jacoco.core.internal.analysis.CachingClassAnalyzer
import org.jacoco.core.internal.analysis.ClassCoverageImpl
import org.jacoco.core.internal.analysis.StringPool
import org.jacoco.core.internal.data.CRC64
import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.instr.InstrSupport
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An [AnalyzerCache] instance processes a set of Java class/jar/war/... files and builds a [ ] for each of the classes.
 *
 *
 * For every class that gets found [.analyzeClass] is called. A class is identified by its class ID which
 * is a CRC64 checksum of the classfile. We process each class with [CachingClassAnalyzer] to fill a [ ].
 */
class AnalyzerCache
/** Creates a new analyzer filling the given cache.  */(
	/** The probes cache.  */
	private val probesCache: ProbesCache, locationIncludeFilter: ClasspathWildcardIncludeFilter,
	logger: ILogger
) : FilteringAnalyzer(null, null, locationIncludeFilter, logger) {
	private val stringPool: StringPool = StringPool()

	/**
	 * Analyses the given class. Instead of the original implementation in [Analyzer.analyzeClass] we
	 * don't use concrete execution data, but instead build a probe cache to speed up repeated lookups.
	 */
	override fun analyzeClass(source: ByteArray) {
		val classId: Long = CRC64.classId(source)
		if (probesCache.containsClassId(classId)) {
			return
		}
		val reader: ClassReader = InstrSupport.classReaderFor(source)
		val classCoverageLookup: ClassCoverageLookup = probesCache.createClass(classId, reader.getClassName())

		// Dummy class coverage object that allows us to subclass ClassAnalyzer with CachingClassAnalyzer and reuse its
		// IFilterContext implementation
		val dummyClassCoverage: ClassCoverageImpl = ClassCoverageImpl(
			reader.getClassName(),
			classId, false
		)

		val classAnalyzer: CachingClassAnalyzer = CachingClassAnalyzer(
			classCoverageLookup, dummyClassCoverage,
			stringPool
		)
		val visitor: ClassVisitor = ClassProbesAdapter(classAnalyzer, false)
		reader.accept(visitor, 0)
	}

	/**
	 * Adds caching for jar files to the analyze jar functionality.
	 */
	@Throws(IOException::class)
	override fun analyzeJar(input: InputStream, location: String): Int {
		val jarId: Long = CRC64.classId(Files.readAllBytes(Paths.get(location)))
		val probesCountForJarId: Int = probesCache.countForJarId(jarId)
		if (probesCountForJarId != 0) {
			return probesCountForJarId
		}
		val count: Int = super.analyzeJar(input, location)
		probesCache.addJarId(jarId, count)
		return count
	}
}
