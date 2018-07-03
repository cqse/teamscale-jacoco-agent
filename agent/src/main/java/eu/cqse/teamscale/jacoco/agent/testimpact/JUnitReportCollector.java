package eu.cqse.teamscale.jacoco.agent.testimpact;

import eu.cqse.teamscale.jacoco.agent.ITestListener;
import eu.cqse.teamscale.jacoco.agent.testimpact.JUnitReport.TestCase;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.testwise.model.TestwiseCoverage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.string.StringUtils;
import spark.Request;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

import static eu.cqse.teamscale.jacoco.agent.testimpact.TestImpactAgent.INTERNAL_ID_QUERY_PARAM;
import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JUNIT;

/**
 * Test listener, which is capable of generating JUnit reports for the tests that have been executed.
 */
public class JUnitReportCollector implements ITestListener {

	/** The logger. */
	protected final Logger logger = LogManager.getLogger(this);

	/** Contains all test cases that have been executed so far. */
	private JUnitReport report = new JUnitReport();

	/** The start time of the currently running test. */
	private long startTimestamp = 0L;

	/** Constructor. */
	public JUnitReportCollector() {
	}

	@Override
	public void onTestStart(Request request, Dump dump) {
		startTimestamp = System.currentTimeMillis();
	}

	@Override
	public void onTestFinish(Request request, Dump dump) {
		long endTimestamp = System.currentTimeMillis();
		double durationInSeconds = (endTimestamp - startTimestamp) / 1000.0;

		if (!request.queryParams().contains(INTERNAL_ID_QUERY_PARAM)) {
			logger.error("The query does not contain the required query parameter '" + INTERNAL_ID_QUERY_PARAM + "'");
		}
		String internalId = request.queryParams(INTERNAL_ID_QUERY_PARAM);
		String className = StringUtils.removeLastPart(internalId, '/');
		String testName = StringUtils.getLastPart(internalId, '/');
		TestCase testCase = new TestCase(className, testName, durationInSeconds);
		String failure = "";
		if (!StringUtils.isEmpty(failure)) {
			testCase.failure = new TestCase.Failure(failure);
		}
		report.testCaseList.add(testCase);
	}

	@Override
	public void onDump(IXmlStore store) {
		StringWriter xmlStringWriter = new StringWriter();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(TestwiseCoverage.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(report, xmlStringWriter);
		} catch (JAXBException e) {
			logger.error("Converting JUnit report failed!", e);
		}

		store.store(xmlStringWriter.toString(), JUNIT);
		report.testCaseList.clear();
	}
}

