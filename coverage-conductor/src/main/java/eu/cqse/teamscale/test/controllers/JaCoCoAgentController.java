package eu.cqse.teamscale.test.controllers;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Translates test start and finish event into actions for the locally running jacoco agent.
 */
public class JaCoCoAgentController {

	/** Singleton instance for this class.  */
	private static JaCoCoAgentController singleton;

	/** Reference to the jacoco agent. */
	private final IAgent agent;

	/** Constructor. */
	private JaCoCoAgentController(IAgent agent) {
		this.agent = agent;
	}

	/** Returns a singleton instance of the controller. */
	public static JaCoCoAgentController getInstance() {
		if (singleton == null) {
			try {
				singleton = new JaCoCoAgentController(RT.getAgent());
			} catch (Exception | NoClassDefFoundError e) {
				throw new JacocoControllerError("Unable to access JaCoCo Agent.", e);
			}
		}
		return singleton;
	}

	/**
	 * Called when a test starts.
	 * Resets coverage and sets the session id.
	 */
	public void onTestStart(String testId) {
		// Reset coverage generated in between the test runs
		agent.reset();
		agent.setSessionId(testId);
	}

	/**
	 * Called when a test finished.
	 * Dumps the coverage of the test to the output file.
	 */
	public void onTestFinish(String testId) {
		try {
			agent.dump(true);

			// Set session id to empty string after test case to work around dump on exit
			// Otherwise the coverage generated after the last test leads to another
			// duplicate session named like the last test that is written on exit
			agent.setSessionId("");
		} catch (IOException e) {
			throw new JacocoControllerError(e);
		}
	}
}
