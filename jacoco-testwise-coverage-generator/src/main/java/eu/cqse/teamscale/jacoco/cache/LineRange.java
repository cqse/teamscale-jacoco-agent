package eu.cqse.teamscale.jacoco.cache;

import org.jacoco.core.analysis.ISourceNode;

public class LineRange {
    private int start = ISourceNode.UNKNOWN_LINE;
    private int end = ISourceNode.UNKNOWN_LINE;

    void adjustToContain(int line) {
        if (start > line || end == ISourceNode.UNKNOWN_LINE) {
            start = line;
        }
        if (end < line) {
            end = line;
        }
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
