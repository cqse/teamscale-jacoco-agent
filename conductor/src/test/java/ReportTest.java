import eu.cqse.teamscale.jacoco.report.ReportWriter;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class ReportTest {

    @Test
    public void testReportGeneration() throws IOException {
        final ReportWriter writer = new ReportWriter(Collections.singleton(getFile("AliasRule.class")));
        ByteArrayOutputStream report = new ByteArrayOutputStream();
        writer.startReport(report);
        writer.readExecutionData(getFile( "test.exec"));
        writer.finishReport();
        assertEquals(removeWhiteSpaces(getFileContent("expected.txt")), removeWhiteSpaces(report.toString()));
    }

    private File getFile(String fileName) {
        return new File(getClass().getClassLoader().getResource(fileName).getFile());
    }

    private String getFileContent(String fileName) {
        StringBuilder result = new StringBuilder("");

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    private String removeWhiteSpaces(String input) {
        return input.replaceAll(">\\n*", ">\n");
    }
}
