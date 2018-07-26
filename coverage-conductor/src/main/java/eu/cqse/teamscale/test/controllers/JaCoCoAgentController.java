package eu.cqse.teamscale.test.controllers;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JaCoCoAgentController {

	private static JaCoCoAgentController singleton;

	private final IAgent agent;
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private JaCoCoAgentController(IAgent agent) {
		this.agent = agent;
	}

	public static synchronized JaCoCoAgentController getInstance() {
		if (singleton == null) {
			try {
				singleton = new JaCoCoAgentController(RT.getAgent());
			} catch (Exception | NoClassDefFoundError e) {
				throw new JacocoControllerError("Unable to access JaCoCo Agent.", e);
			}
		}
		return singleton;
	}

	public synchronized void onTestStart(String testId) {
		// Make sure test case execution does not overlap
		lock.writeLock().lock();

		// Reset coverage generated in between the test runs
		agent.reset();

		// Set the session id
		agent.setSessionId(testId);
	}

	public synchronized void onTestFinish(String testId) {
		try {
			agent.dump(true);

			// Set session id to empty string after test case to work around dump on exit
			// Otherwise the coverage generated after the last test leads to another
			// duplicate session named like the last test that is written on exit
			agent.setSessionId("");
		} catch (IOException e) {
			throw new JacocoControllerError(e);
		} finally {
			lock.writeLock().unlock();
		}
	}
}
