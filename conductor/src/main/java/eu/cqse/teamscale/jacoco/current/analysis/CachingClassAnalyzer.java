package eu.cqse.teamscale.jacoco.current.analysis;

import eu.cqse.teamscale.jacoco.common.cache.ProbeLookup;
import org.jacoco.core.internal.analysis.StringPool;
import org.jacoco.core.internal.flow.ClassProbesVisitor;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Analyzes a class to reconstruct probe information.
 * <p>
 * It's core is a copy of {@link org.jacoco.core.internal.analysis.ClassAnalyzer} that has been
 * extended with caching functionality to speed up report generation.
 */
public class CachingClassAnalyzer extends ClassProbesVisitor {

    private final StringPool stringPool;
    private final ProbeLookup probeLookup;

    /**
     * Creates a new analyzer that builds coverage data for a class.
     *
     * @param probeLookup cache for the class' probes
     * @param stringPool  shared pool to minimize the number of {@link String} instances
     */
    public CachingClassAnalyzer(final StringPool stringPool, ProbeLookup probeLookup) {
        this.stringPool = stringPool;
        this.probeLookup = probeLookup;
    }

    @Override
    public void visitSource(final String source, final String debug) {
        probeLookup.setSourceFileName(stringPool.get(source));
    }

    @Override
    public MethodProbesVisitor visitMethod(final int access, final String name,
                                           final String desc, final String signature, final String[] exceptions) {

        if (isMethodFiltered(access, name)) {
            return null;
        }

        return new CachingMethodAnalyzer(probeLookup);
    }

    // TODO: Use filter hook in future
    private boolean isMethodFiltered(final int access, final String name) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0
                && !name.startsWith("lambda$");
    }

    @Override
    public void visitTotalProbeCount(final int count) {
        // nothing to do
    }

}
