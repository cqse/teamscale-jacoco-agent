package eu.cqse.teamscale.jacoco.agent.testimpact;

import com.google.gson.Gson;
import eu.cqse.teamscale.client.TestDetails;
import eu.cqse.teamscale.jacoco.agent.ITestListener;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.report.jacoco.dump.Dump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;

import java.util.ArrayList;
import java.util.List;

import static eu.cqse.teamscale.client.EReportFormat.TEST_LIST;
import static eu.cqse.teamscale.jacoco.agent.testimpact.TestImpactAgent.getTestDetailsFromRequest;

/**
 * Test listener, which is capable of generating JUnit reports for the tests that have been executed.
 */
public class TestDetailsCollector implements ITestListener {

	/** The logger. */
	protected final Logger logger = LogManager.getLogger(this);

	/** Contains all test details for all tests that have been executed so far. */
	private final List<TestDetails> testDetailsList = new ArrayList<>();

	@Override
	public void onTestStart(Request request, Dump dump) {
		TestDetails testDetails = getTestDetailsFromRequest(request, logger);
		if (testDetails != null) {
			this.testDetailsList.add(testDetails);
		}
	}

	@Override
	public void onTestFinish(Request request, Dump dump) {
		// Nothing to do here since we have already saved the test details for the current test in #onTestStart
	}

	@Override
	public void onDump(IXmlStore store) {
		String reportString = new Gson().toJson(testDetailsList);
		store.store(reportString, TEST_LIST);
		testDetailsList.clear();
	}
}
