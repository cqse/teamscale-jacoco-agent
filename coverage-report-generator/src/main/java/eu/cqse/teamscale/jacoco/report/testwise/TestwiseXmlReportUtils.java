package eu.cqse.teamscale.jacoco.report.testwise;

import eu.cqse.teamscale.jacoco.report.testwise.model.TestwiseCoverage;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/** Utilities for testwise coverage reports. */
public class TestwiseXmlReportUtils {

	/** Converts to given testwise coverage to an XML report and writes it to the given output stream. */
	public static void writeReportToStream(OutputStream output, TestwiseCoverage testwiseCoverage) throws IOException {
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
