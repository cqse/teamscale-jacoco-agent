package eu.cqse.teamscale.jacoco.report;

import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.util.Benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface IJaCoCoReportGenerator {

    /** Encoding for UTF-8. */
    String UTF8_ENCODING = "UTF-8";

    /**
     * Creates the report.
     */
    default String convert(Dump dump) throws IOException {
        try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            convertToReport(output, dump);
            return output.toString(UTF8_ENCODING);
        }
    }

    void convertToReport(OutputStream output, Dump dump) throws IOException;
}
