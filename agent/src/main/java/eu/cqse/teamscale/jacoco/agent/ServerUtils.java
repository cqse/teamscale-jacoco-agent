package eu.cqse.teamscale.jacoco.agent;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerUtils {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /** HTTP status code to indicate that the request was successful and is not returning any content. */
    private static final int STATUS_NO_CONTENT = 204;

    /** HTTP status code to indicate a problem with the clients request. */
    private static final int STATUS_BAD_REQUEST = 400;

    /** HTTP status code to indicate a problem with processing the request caused by a problem on the server side. */
    private static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    /** Response length to indicate an empty response. */
    private static final int NO_RESPONSE_LENGTH = -1;

    public static Map<String, List<String>> getRequestParameters(final URI requestUri) {
        final Map<String, List<String>> requestParameters = new LinkedHashMap<>();
        final String requestQuery = requestUri.getRawQuery();
        if (requestQuery != null) {
            final String[] rawRequestParameters = requestQuery.split("[&;]", -1);
            for (final String rawRequestParameter : rawRequestParameters) {
                final String[] requestParameter = rawRequestParameter.split("=", 2);
                final String requestParameterName = decodeUrlComponent(requestParameter[0]);
                requestParameters.putIfAbsent(requestParameterName, new ArrayList<>());
                final String requestParameterValue = requestParameter.length > 1 ? decodeUrlComponent(requestParameter[1]) : null;
                requestParameters.get(requestParameterName).add(requestParameterValue);
            }
        }
        return requestParameters;
    }

    private static String decodeUrlComponent(final String urlComponent) {
        try {
            return URLDecoder.decode(urlComponent, CHARSET.name());
        } catch (final UnsupportedEncodingException ex) {
            throw new InternalError(ex);
        }
    }

    public static void respondSuccess(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(STATUS_NO_CONTENT, NO_RESPONSE_LENGTH);
    }

    public static void respondBadRequest(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(STATUS_BAD_REQUEST, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static void respondInternalServerError(HttpExchange httpExchange, Throwable exception) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString(); // stack trace as a string
        respondInternalServerError(httpExchange, stackTrace);
    }

    public static void respondInternalServerError(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(STATUS_INTERNAL_SERVER_ERROR, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}