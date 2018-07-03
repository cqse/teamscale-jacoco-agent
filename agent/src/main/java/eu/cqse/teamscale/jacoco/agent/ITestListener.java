package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.dump.Dump;
import spark.Request;

/** Interface for classes that need to react upon test execution events */
public interface ITestListener {

	/**
	 * Called when a test is about to start.
	 *
	 * @param request The HTTP request that was sent to indicate the test start.
	 * @param dump    The coverage dump from everything that was collected since the end of the last test
	 *                or since the system startup (if this is the first test.)
	 */
	void onTestStart(Request request, Dump dump);

	/**
	 * Called when a test has just ended.
	 *
	 * @param request The HTTP request that was sent to indicate the test end.
	 * @param dump    The coverage dump from everything that was covered since the test has started.
	 */
	void onTestFinish(Request request, Dump dump);

	/**
	 * Called whenever artifacts should be generated and dumped to the store.
	 * Either when triggered from an external dump command, when the interval has exceeded or the system is
	 * currently being shut down.
	 */
	void onDump(IXmlStore store);
}
