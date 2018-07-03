package eu.cqse.teamscale.jacoco.agent.testimpact;

import com.google.gson.Gson;
import eu.cqse.teamscale.jacoco.agent.ITestListener;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.dump.Dump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.string.StringUtils;
import spark.Request;

import java.util.ArrayList;
import java.util.List;

import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TEST_LIST;
import static eu.cqse.teamscale.jacoco.agent.testimpact.TestImpactAgent.INTERNAL_ID_QUERY_PARAM;
import static eu.cqse.teamscale.jacoco.agent.testimpact.TestImpactAgent.TEST_ID_PARAMETER;

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
		if (!request.queryParams().contains(INTERNAL_ID_QUERY_PARAM)) {
			logger.error("The query does not contain the required query parameter '" + INTERNAL_ID_QUERY_PARAM + "'");
			return;
		}
		String internalId = request.queryParams(INTERNAL_ID_QUERY_PARAM);
		String externalId = request.params(TEST_ID_PARAMETER);
		String testName = StringUtils.getLastPart(internalId, '/');
		TestDetails testDetails = new TestDetails(externalId, internalId, null, testName);
		this.testDetailsList.add(testDetails);
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
