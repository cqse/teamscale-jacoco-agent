package eu.cqse.teamscale.jacoco.analysis;

import eu.cqse.teamscale.jacoco.cache.ProbeLookup;
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

    /** The probes cache. */
    private final ProbeLookup probeLookup;

    /**
     * Creates a new analyzer that builds coverage data for a class.
     *
     * @param probeLookup cache for the class' probes
     */
    public CachingClassAnalyzer(ProbeLookup probeLookup) {
        this.probeLookup = probeLookup;
    }

    @Override
    public void visitSource(String source, String debug) {
        probeLookup.setSourceFileName(source);
    }

    @Override
    public MethodProbesVisitor visitMethod(final int access, final String name,
                                           final String desc, final String signature, final String[] exceptions) {
        return new CachingMethodAnalyzer(probeLookup);
    }

    @Override
    public void visitTotalProbeCount(final int count) {
        // nothing to do
    }
}
