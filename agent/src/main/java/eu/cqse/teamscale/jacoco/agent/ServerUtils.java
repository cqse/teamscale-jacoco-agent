package eu.cqse.teamscale.jacoco.agent;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Utilities for working with {@link com.sun.net.httpserver.HttpServer}. */
public class ServerUtils {

    /** HTTP status code to indicate that the request was successful and is not returning any content. */
    private static final int STATUS_NO_CONTENT = 204;

    /** HTTP status code to indicate a problem with the clients request. */
    private static final int STATUS_BAD_REQUEST = 400;

    /** HTTP status code to indicate a problem with processing the request caused by a problem on the server side. */
    private static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    /** Response length to indicate an empty response. */
    private static final int NO_RESPONSE_LENGTH = -1;

    /** Sets the response header to indicate a successful request without any response. */
    public static void respondSuccess(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(STATUS_NO_CONTENT, NO_RESPONSE_LENGTH);
    }

    /** Responds the query with a bad request message. */
    public static void respondBadRequest(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(STATUS_BAD_REQUEST, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /** Responds the query with an internal server error message. */
    public static void respondInternalServerError(HttpExchange httpExchange, Throwable exception) throws IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();
        respondInternalServerError(httpExchange, stackTrace);
    }

    /** Responds the query with an internal server error message. */
    public static void respondInternalServerError(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(STATUS_INTERNAL_SERVER_ERROR, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}