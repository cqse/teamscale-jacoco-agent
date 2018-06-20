/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.cqse.teamscale.jacoco.agent.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.testwise.TestwiseXmlReportGenerator;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.data.SessionInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static eu.cqse.teamscale.jacoco.agent.ServerUtils.respondBadRequest;
import static eu.cqse.teamscale.jacoco.agent.ServerUtils.respondInternalServerError;
import static eu.cqse.teamscale.jacoco.agent.ServerUtils.respondSuccess;

/**
 * A wrapper around the JaCoCo Java agent that starts a HTTP server and listens for test events.
 */
public class ServerAgent extends AgentBase {

    /** The agent options. */
    private AgentOptions options;

    /** The http server instance. */
    private HttpServer server;

    /** Converts binary data to XML. */
    private TestwiseXmlReportGenerator generator;

    /** Timestamp at which the report was dumped the last time. */
    private long lastDumpTimestamp = System.currentTimeMillis();

    /** List of dumps, one for each test. */
    private List<Dump> dumps = new ArrayList<>();

    /** Constructor. */
    public ServerAgent(AgentOptions options) {
        super(options);
        this.options = options;
        generator = new TestwiseXmlReportGenerator(options.getClassDirectoriesOrZips());

        logger.info("Dumping every {} minutes.", options.getDumpIntervalInMinutes());
    }

    /**
     * Starts the http server, which waits for information about started and finished tests.
     */
    public void startServer() throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(options.getHttpServerPort());
        server = HttpServer.create(inetSocketAddress, 0);
        server.createContext("/test/start", httpExchange -> handleTest(httpExchange, this::handleTestStart));
        server.createContext("/test/end", httpExchange -> handleTest(httpExchange, this::handleTestEnd));
        server.setExecutor(null);
        server.start();
        logger.info("Listening for test events on port {}.", options.getHttpServerPort());
    }

    /**
     * Generic handler for a test start or end HTTP call, which takes care of argument paring and error handling.
     * For handling the specific intent of the call the given handler is called.
     */
    private void handleTest(HttpExchange httpExchange, ITestIdHandler handler) throws IOException {
        String testId = getTestId(httpExchange.getRequestURI());
        if (testId.isEmpty()) {
            logger.error("Invalid request " + httpExchange.getRequestURI());
            respondBadRequest(httpExchange, "No test id given! Expected /test/(start|end)/<test id>!");
            return;
        }
        try {
            handler.process(testId);
        } catch (Exception e) {
            logger.error(e);
            respondInternalServerError(httpExchange, e);
            return;
        }
        respondSuccess(httpExchange);
    }

    /** Extracts the test ID from the given URI. */
    private static String getTestId(URI requestURI) {
        String path = requestURI.getPath();
        return path.replaceFirst("/test/(start|end)/?", "");
    }

    /** Handles the start of a new test case by setting the session ID. */
    private void handleTestStart(String testId) throws DumpException {
        logger.debug("Start test " + testId);
        controller.reset();
    }

    /** Handles the end of a test case by resetting the session ID. */
    private void handleTestEnd(String testId) throws DumpException {
        logger.debug("End test " + testId);
        controller.setSessionId(testId);
        dumps.add(controller.dumpAndReset());

        // If the last dump was longer ago than the specified interval dump report
        if (lastDumpTimestamp + options.getDumpIntervalInMillis() < System.currentTimeMillis()) {
            dumpReport();
            lastDumpTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Dumps the current execution data, converts it and writes it to the
     * {@link #store}. Logs any errors, never throws an exception.
     */
    private void dumpReport() {
        logger.debug("Starting dump");

        try {
            dumpReportUnsafe();
        } catch (Throwable t) {
            // we want to catch anything in order to avoid killing the regular job
            logger.error("Dump job failed with an exception. Retrying later", t);
        }
    }

    /**
     * Performs the actual dump but does not handle e.g. OutOfMemoryErrors.
     */
    private void dumpReportUnsafe() {
        String xml;
        try {
            xml = generator.convert(dumps);
        } catch (IOException e) {
            logger.error("Converting binary dumps to XML failed", e);
            return;
        }

        store.store(xml);
    }

    @Override
    protected void prepareShutdown() {
        dumpReport();
        server.stop(0);
    }

    /** Callback interface for handling a test related HTTP call. */
    interface ITestIdHandler {
        void process(String testId) throws Exception;
    }
}
