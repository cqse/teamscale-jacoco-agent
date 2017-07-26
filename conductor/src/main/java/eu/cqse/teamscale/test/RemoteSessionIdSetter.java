package eu.cqse.teamscale.test;

import eu.cqse.teamscale.test.controllers.JaCoCoRemoteJMXControllerJNI;

import java.io.IOException;

public class RemoteSessionIdSetter {

    public static void main(String[] args) throws IOException {
        for (int i = 1; i < 2; i++) {
            JaCoCoRemoteJMXControllerJNI.onTestStart("localhost", 9999, args[0], args[1] + i);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            JaCoCoRemoteJMXControllerJNI.onTestFinish("localhost", 9999, args[0], args[1] + i);
        }
    }
}
