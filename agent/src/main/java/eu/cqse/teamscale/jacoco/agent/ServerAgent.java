/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.cqse.teamscale.jacoco.agent.dump.Dump;
import eu.cqse.teamscale.jacoco.agent.dump.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.agent.report.XmlReportGenerator;

import java.io.IOException;
import java.net.InetSocketAddress;

import static eu.cqse.teamscale.jacoco.agent.ServerUtils.respondBadRequest;
import static eu.cqse.teamscale.jacoco.agent.ServerUtils.respondInternalServerError;
import static eu.cqse.teamscale.jacoco.agent.ServerUtils.respondSuccess;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
public class ServerAgent extends AgentBase {

    /** Converts binary data to XML. */
    private final XmlReportGenerator generator;

    /** The port at which the http server should be started. */
    private int httpServerPort;

    /** The http server instance. */
    private HttpServer server;

    /** Constructor. */
    public ServerAgent(AgentOptions options) {
        super(options);

        generator = new XmlReportGenerator(options.getClassDirectoriesOrZips(), options.getLocationIncludeFilter(),
                options.shouldIgnoreDuplicateClassFiles());

        httpServerPort = options.getHttpServerPort();

        logger.info("Listening for test events on port {}. Storage method: {}", httpServerPort, options.getDumpIntervalInMinutes(),
                store.describe());
    }

    /**
     * Starts the http server, which waits for information about started and finished tests.
     */
    public void startServer() throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(httpServerPort);
        server = HttpServer.create(inetSocketAddress, 0);
        server.createContext("/test/start", httpExchange -> handleTest(httpExchange, this::handleTestStart));
        server.createContext("/test/end", httpExchange -> handleTest(httpExchange, this::handleTestEnd));
        server.setExecutor(null);
        server.start();
    }

    private void handleTest(HttpExchange httpExchange, ITestIdHandler handler) throws IOException {
        String testId = getTestId(httpExchange);
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

    private static String getTestId(HttpExchange httpExchange) {
        String path = httpExchange.getRequestURI().getPath();
        return path.replaceFirst("/test/(start|end)/?", "");
    }

    private void handleTestStart(String testId) throws DumpException {
        logger.debug("Start test " + testId);
        controller.setSessionId(testId);
    }

    private void handleTestEnd(String testId) throws DumpException {
        logger.debug("End test " + testId);
        controller.setSessionId("");
    }

    @Override
    protected void prepareShutdown() {
        dump();
        server.stop(0);
    }

    @Override
    protected String generateReport(Dump dump) throws IOException {
        return null;
    }

    interface ITestIdHandler {
        void process(String testId) throws Exception;
    }
}
