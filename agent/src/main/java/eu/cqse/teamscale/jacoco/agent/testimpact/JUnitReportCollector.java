package eu.cqse.teamscale.jacoco.agent.testimpact;

import eu.cqse.teamscale.jacoco.agent.ITestListener;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.report.jacoco.dump.Dump;
import eu.cqse.teamscale.report.junit.JUnitReport;
import eu.cqse.teamscale.report.junit.JUnitReport.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.string.StringUtils;
import spark.Request;

import javax.xml.bind.JAXBException;
import java.util.Arrays;

import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JUNIT;
import static eu.cqse.teamscale.jacoco.agent.testimpact.TestImpactAgent.getTestDetailsFromRequest;
import static eu.cqse.teamscale.report.junit.JUnitReportGenerator.generateJUnitReport;

/**
 * Test listener, which is capable of generating JUnit reports for the tests that have been executed.
 */
public class JUnitReportCollector implements ITestListener {

	/** The query parameter is use for attaching a test result. */
	private static final String QUERY_RESULT_PARAM = "result";

	/** The logger. */
	protected final Logger logger = LogManager.getLogger(this);

	/** Contains test execution information of all tests that have been executed so far in JUnit report format. */
	private final JUnitReport report = new JUnitReport();

	/** The start time of the currently running test. */
	private long startTimestamp = 0L;

	/** The currently running test or null if there is no test started. */
	private TestCase currentTestCase = null;

	@Override
	public void onTestStart(Request request, Dump dump) {
		startTimestamp = System.currentTimeMillis();

		TestDetails testDetails = getTestDetailsFromRequest(request, logger);
		if (testDetails == null) {
			currentTestCase = null;
			return;
		}
		String internalId = testDetails.internalId;
		String className = StringUtils.removeLastPart(internalId, '/');
		String testName = StringUtils.getLastPart(internalId, '/');
		currentTestCase = new TestCase(className, testName);
	}

	@Override
	public void onTestFinish(Request request, Dump dump) {
		if (currentTestCase == null) {
			return;
		}

		long endTimestamp = System.currentTimeMillis();
		currentTestCase.setDurationInSeconds((endTimestamp - startTimestamp) / 1000.0);

		switch (getResult(request)) {
			case PASSED:
				break;
			case SKIPPED:
				currentTestCase.setSkipped(request.body());
			case IGNORED:
				currentTestCase.setIgnored(true);
			case ERROR:
				currentTestCase.setError(request.body());
			case FAILURE:
				currentTestCase.setFailure(request.body());
		}
		report.add(currentTestCase);
	}

	/** Parses the result from the query. */
	private ETestExecutionResult getResult(Request request) {
		String result = request.params(QUERY_RESULT_PARAM);
		if (result != null) {
			try {
				return ETestExecutionResult.valueOf(result);
			} catch (IllegalArgumentException e) {
				logger.error("'" + result + "' is not a valid test execution result! Use can use any of " + Arrays
						.toString(ETestExecutionResult.values()), e);
			}
		}
		return ETestExecutionResult.PASSED;
	}

	@Override
	public void onDump(IXmlStore store) {
		String xml;
		try {
			xml = generateJUnitReport(report);
		} catch (JAXBException e) {
			logger.error("Converting JUnit report failed!", e);
			return;
		}

		store.store(xml, JUNIT);
		report.reset();
	}
}

