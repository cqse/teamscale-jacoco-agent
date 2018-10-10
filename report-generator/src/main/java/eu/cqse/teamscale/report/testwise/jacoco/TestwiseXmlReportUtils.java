package eu.cqse.teamscale.report.testwise.jacoco;

import eu.cqse.teamscale.report.testwise.model.TestwiseCoverage;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Utilities for testwise coverage reports. */
public class TestwiseXmlReportUtils {

	/** Converts to given testwise coverage to an XML report and writes it to the given file. */
	public static void writeReportToFile(File report, TestwiseCoverageReport testwiseCoverage) throws IOException {
		File directory = report.getParentFile();
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw new IOException("Failed to create directory " + directory.getAbsolutePath());
		}
		try (FileOutputStream output = new FileOutputStream(report)) {
			TestwiseXmlReportUtils.writeReportToStream(output, testwiseCoverage);
		}
	}

	/** Converts to given testwise coverage to an XML report and writes it to the given output stream. */
	public static String getReportAsString(TestwiseCoverageReport testwiseCoverage) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		writeReportToStream(output, testwiseCoverage);
		return output.toString(FileSystemUtils.UTF8_ENCODING);
	}

	/** Converts to given testwise coverage to an XML report and writes it to the given output stream. */
	public static void writeReportToStream(OutputStream output, TestwiseCoverageReport testwiseCoverage) throws IOException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(TestwiseCoverage.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(testwiseCoverage, output);
		} catch (JAXBException e) {
			throw new IOException("Converting testwise coverage to XML failed!", e);
		}
	}
}
