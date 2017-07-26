package eu.cqse.teamscale.test.controllers;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static eu.cqse.teamscale.jacoco.report.XMLCoverageWriter.SESSION_ID_SEPARATOR;

public class JacocoLocalController implements IJaCoCoController {

    private static final String ERROR = "Unable to access JaCoCo Agent - make sure that you use JaCoCo and version not lower than 0.7.9.";

    private final IAgent agent;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static JacocoLocalController singleton;
    private long startTime;
    private String clazz = "";
    private String method = "";
    public boolean doWriteImmediately = true;

    private JacocoLocalController() {
        try {
            this.agent = RT.getAgent();
        } catch (Exception | NoClassDefFoundError e) {
            throw new JacocoControllerError(ERROR, e);
        }
    }

    public static synchronized JacocoLocalController getInstance() {
        if (singleton == null) {
            singleton = new JacocoLocalController();
        }
        return singleton;
    }

    @Override
    public synchronized void onTestStart(String className, String testName) {
        lock.writeLock().lock();

        if (!className.equals(clazz) || !testName.equals(method)) {
            if (clazz != null && method != null) {
                writeFinishTest(clazz, method);
            }

            // Dump coverage between tests
            dump("");
            startTime = System.currentTimeMillis();
            clazz = className;
            method = testName;
        }
    }

    @Override
    public synchronized void onTestFinish(String className, String testName) {
        try {
            if (doWriteImmediately) {
                writeFinishTest(className, testName);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void writeNow() {
        if (clazz != null && method != null) {
            writeFinishTest(clazz, method);
            clazz = null;
            method = null;
        }
    }

    private void writeFinishTest(String className, String testName) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        dump(className + SESSION_ID_SEPARATOR + testName + SESSION_ID_SEPARATOR + duration);
    }

    private void dump(String sessionId) {
        agent.setSessionId(sessionId);
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