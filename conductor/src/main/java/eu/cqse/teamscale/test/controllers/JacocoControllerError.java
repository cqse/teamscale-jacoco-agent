package eu.cqse.teamscale.test.controllers;

public class JacocoControllerError extends Error {
    public JacocoControllerError(String message) {
        super(message);
    }

    public JacocoControllerError(String message, Throwable cause) {
        super(message, cause);
    }

    public JacocoControllerError(Throwable cause) {
        super(cause);
    }
}
