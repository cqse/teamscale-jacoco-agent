package eu.cqse.teamscale.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SuppressWarnings("WeakerAccess")
public class CoverageConfiguration {

    public Project project = new Project();
    public Server teamscale = new Server();
    public JaCoCoAgent jacocoAgent = new JaCoCoAgent();

    private CoverageConfiguration() {
    }

    public static CoverageConfiguration load(String configFile) {
        return load(new File(configFile));
    }

    public static CoverageConfiguration load(File configFile) {
        Properties prop = new Properties();

        try (InputStream input = new FileInputStream(configFile)) {
            prop.load(input);

            CoverageConfiguration config = new CoverageConfiguration();
            config.project.rootDirectory = new File(prop.getProperty("project.directory"));
            config.project.classesDirectory = new File(config.project.rootDirectory, prop.getProperty("project.directory.classes", ""));

            config.jacocoAgent.address = prop.getProperty("jacoco_agent.address", "localhost");
            config.jacocoAgent.port = Integer.parseInt(prop.getProperty("jacoco_agent.port", "6300"));

            config.teamscale.url = prop.getProperty("teamscale.url", "http://localhost:8080");
            config.teamscale.project = prop.getProperty("teamscale.project");
            config.teamscale.userName = prop.getProperty("teamscale.user.name");
            config.teamscale.userAccessToken = prop.getProperty("teamscale.user.access_token");
            config.teamscale.coveragePartition = prop.getProperty("teamscale.coverage.partition", "COVERAGE");
            config.teamscale.coverageMessage = prop.getProperty("teamscale.coverage.message", "Coverage upload");
            return config;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
