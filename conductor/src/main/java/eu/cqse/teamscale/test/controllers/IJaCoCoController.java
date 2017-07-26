package eu.cqse.teamscale.test.controllers;

public interface IJaCoCoController {
    void onTestStart(String className, String testName);
    void onTestFinish(String className, String testName);
}
