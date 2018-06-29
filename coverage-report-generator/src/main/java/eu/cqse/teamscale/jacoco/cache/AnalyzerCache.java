package eu.cqse.teamscale.jacoco.cache;

import eu.cqse.teamscale.jacoco.analysis.CachingClassAnalyzer;
import eu.cqse.teamscale.jacoco.report.linebased.FilteringAnalyzer;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.internal.flow.ClassProbesAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * An {@link AnalyzerCache} instance processes a set of Java class/jar/war/... files and
 * builds a {@link ProbeLookup} for each of the classes.
 * <p>
 * The class basically needs to override {@link org.jacoco.core.analysis.Analyzer#analyzeClass(byte[])}.
 * Since the method is private we need to override and copy the implementations of all methods that call this method,
 * which are {@link org.jacoco.core.analysis.Analyzer#analyzeClass(ClassReader)} and
 * {@link org.jacoco.core.analysis.Analyzer#analyzeClass(byte[], String)}. When updating we just need to make sure that
 * no additional methods have been added to {@link org.jacoco.core.analysis.Analyzer}, which call the private method
 * internally.
 */
public class AnalyzerCache extends FilteringAnalyzer {

	/** The probes cache. */
	private final ProbesCache probesCache;

	/** Creates a new analyzer filling the given cache. */
	public AnalyzerCache(ProbesCache probesCache, Predicate<String> locationIncludeFilter) {
		super(null, null, locationIncludeFilter);
		this.probesCache = probesCache;
	}

	/**
	 * Analyses the given class. Instead of the original implementation in
	 * {@link org.jacoco.core.analysis.Analyzer#analyzeClass(byte[])} we don't use concrete execution data, but
	 * instead build a probe cache to speed up repeated lookups.
	 */
	private void analyzeClass(final byte[] source) {
		long classId = CRC64.classId(source);
		if (probesCache.containsClassId(classId)) {
			return;
		}
		final ClassReader reader = new ClassReader(source);
		CachingClassAnalyzer classAnalyzer = new CachingClassAnalyzer(
				probesCache.addClass(classId, reader.getClassName()));
		final ClassVisitor visitor = new ClassProbesAdapter(classAnalyzer, false);
		reader.accept(visitor, 0);
	}

	/**
	 * @inheritDoc <p>
	 * Copy of the method from {@link org.jacoco.core.analysis.Analyzer#analyzeClass(ClassReader)}, because it calls
	 * the private {@link org.jacoco.core.analysis.Analyzer#analyzeClass(byte[])} method, which we therefore cannot
	 * override.
	 */
	@Override
	public void analyzeClass(final ClassReader reader) {
		analyzeClass(reader.b);
	}

	/**
	 * @inheritDoc <p>
	 * Copy of the method from {@link org.jacoco.core.analysis.Analyzer#analyzeClass(byte[], String)}, because it calls
	 * the private {@link org.jacoco.core.analysis.Analyzer#analyzeClass(byte[])} method, which we therefore cannot
	 * override.
	 */
	@Override
	public void analyzeClass(final byte[] buffer, final String location) throws IOException {
		try {
			analyzeClass(buffer);
		} catch (RuntimeException cause) {
			throw new IOException(String.format("Error while analyzing %s.", location), cause);
		}
	}
}