package com.teamscale.jacoco.agent.options.sapnwdi;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Wraps multiple {@link IUploader}s in order to delay uploads until a {@link CommitDescriptor} is asynchronously made
 * available for each application. Whenever a dump happens the coverage is uploaded to all projects for
 * which a corresponding commit has already been found. Uploads for application that have not commit at that time are skipped.
 *
 * This is safe assuming that the marker class is the central entry point for the application and therefore there
 * should not be any relevant coverage for the application as long as the marker class has not been loaded.
 */
public class DelayedSapNwdiMultiUploader implements IUploader {

	private final Logger logger = LoggingUtils.getLogger(this);
	private final BiFunction<CommitDescriptor, SapNwdiApplications.SapNwdiApplication, IUploader> uploaderFactory;
	private final Map<SapNwdiApplications.SapNwdiApplication, IUploader> uploaders = new HashMap<>();

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} to test the asynchronous functionality of this
	 * class.
	 */
	public DelayedSapNwdiMultiUploader(
			BiFunction<CommitDescriptor, SapNwdiApplications.SapNwdiApplication, IUploader> uploaderFactory) {
		this.uploaderFactory = uploaderFactory;
		registerShutdownHook();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (uploaders.isEmpty()) {
				logger.error("The application was shut down before a commit could be found. The recorded coverage" +
						" is lost. You configured the agent to auto-detect the commit via the last modification" +
						" timestamp of an application-specific marker class per Teamscale project.");
			}
		}));
	}

	@Override
	public synchronized void upload(CoverageFile file) {
		if (uploaders.isEmpty()) {
			logger.warn("No commits have been found yet to which coverage should be uploaded. Discarding coverage");
		} else {
			for (IUploader wrappedUploader : uploaders.values()) {
				wrappedUploader.upload(file.acquireReference());
			}
		}
	}

	@Override
	public String describe() {
		if (!uploaders.isEmpty()) {
			return uploaders.values().stream().map(IUploader::describe).collect(Collectors.joining(", "));
		}
		return "Temporary stand-in until commit is resolved";
	}

	/** Sets the commit info detected for the application. */
	public void setCommitForApplication(CommitDescriptor commit, SapNwdiApplications.SapNwdiApplication application) {
		logger.info("Found commit for " + application.markerClass + ": " + commit);
		IUploader uploader = uploaderFactory.apply(commit, application);
		uploaders.put(application, uploader);
	}
}
