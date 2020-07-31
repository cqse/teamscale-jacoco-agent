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
 * Wraps an {@link IUploader} and in order to delay upload until a {@link CommitDescriptor} is asynchronously made
 * available.
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
						" is lost. You configured the agent to auto-detect the commit via the " +
						"'Implementation-Version' entry in the MANIFEST.MF to which the recorded " +
						"coverage should be uploaded to Teamscale.");
			}
		}));
	}

	@Override
	public synchronized void upload(CoverageFile file) {
		if (uploaders.isEmpty()) {
			logger.info("The commit to upload to has not yet been found. Discarding coverage");
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
