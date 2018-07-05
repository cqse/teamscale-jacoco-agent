package eu.cqse.teamscale.jacoco.report.junit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

/** Generator for JUnit reports. */
public class JUnitReportGenerator {

	/**  Generates a XML report from the given JUnitReport object. */
	public static String generateJUnitReport(JUnitReport report) throws JAXBException {
		String xml;
		StringWriter xmlStringWriter = new StringWriter();
		JAXBContext jaxbContext = JAXBContext.newInstance(JUnitReport.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.marshal(report, xmlStringWriter);
		xml = xmlStringWriter.toString();
		return xml;
	}
}
