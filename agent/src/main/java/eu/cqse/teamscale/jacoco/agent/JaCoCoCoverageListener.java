package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService;
import eu.cqse.teamscale.jacoco.dump.Dump;
import eu.cqse.teamscale.jacoco.report.linebased.JaCoCoXmlReportGenerator;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.data.SessionInfoStore;
import spark.Request;

import java.io.IOException;

import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.JACOCO;
import static eu.cqse.teamscale.jacoco.util.LoggingUtils.wrap;

/**
 * Test listener, which is capable of generating line-based JaCoCo coverage reports for the whole system
 * independent from the testwise coverage.
 */
public class JaCoCoCoverageListener implements ITestListener {

	/** The logger. */
	protected final Logger logger = LogManager.getLogger(this);

	/** Generates XML reports from binary execution data. */
	private JaCoCoXmlReportGenerator generator;

	/** Store with all session information. */
	private final SessionInfoStore sessionInfoStore = new SessionInfoStore();

	/** Execution data store holding all coverage including coverage from in between tests. */
	private final ExecutionDataStore executionDataStore = new ExecutionDataStore();

	/** Constructor. */
	public JaCoCoCoverageListener(AgentOptions options, Logger logger) {
		this.generator = new JaCoCoXmlReportGenerator(options.getClassDirectoriesOrZips(),
				options.getLocationIncludeFilter(), options.shouldIgnoreDuplicateClassFiles(), wrap(logger));
	}

	@Override
	public void onTestStart(Request request, Dump dump) {
		appendCoverage(dump);
	}

	@Override
	public void onTestFinish(Request request, Dump dump) {
		appendCoverage(dump);
	}

	/** Append/merge the dump into {@link #sessionInfoStore} and {@link #executionDataStore}. */
	private void appendCoverage(Dump dump) {
		sessionInfoStore.visitSessionInfo(dump.info);
		for (ExecutionData content : dump.store.getContents()) {
			executionDataStore.put(content);
		}
	}

	@Override
	public void onDump(IXmlStore store) {
		String xml;
		try (Benchmark benchmark = new Benchmark("Generating the XML report")) {
			SessionInfo sessionInfo = sessionInfoStore.getMerged("merged");
			xml = generator.convert(new Dump(sessionInfo, executionDataStore));
		} catch (IOException e) {
			logger.error("Converting binary dumps to XML failed", e);
			return;
		}

		store.store(xml, JACOCO);
	}
}
