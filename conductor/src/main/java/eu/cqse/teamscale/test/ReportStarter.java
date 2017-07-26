package eu.cqse.teamscale.test;

import eu.cqse.teamscale.config.Server;
import eu.cqse.teamscale.jacoco.report.XMLReportGenerator;
import eu.cqse.teamscale.test.upload.CommitDescriptor;
import eu.cqse.teamscale.test.upload.TeamscaleClient;
import eu.cqse.teamscale.test.upload.TeamscaleService;

import java.io.File;
import java.io.IOException;

public class ReportStarter {

    private static final File REPORT = new File("jacoco-session-report.xml");

    public static void main(String[] args) throws IOException {
        new XMLReportGenerator(new File("out/artifacts"), new File("jacoco.exec"))
                .writeToFile(REPORT);

        Server server = new Server();
        server.userName = "build";
        server.userAccessToken = "g8BPwNscJAuRWm4qCFFs-7CAwkv671rg";
        server.coverageMessage = "Tosca test coverage upload";
        server.coveragePartition = "COVERAGE";
        server.project = "tricentis-angebotsrechner";
        server.url = "http://10.37.129.2:8181/";
        new TeamscaleClient(server).uploadReport(TeamscaleService.EReportFormat.JACOCO, REPORT,
                new CommitDescriptor("master", 1500020136000L), server.coveragePartition, server.coverageMessage);
    }
}
