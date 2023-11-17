package com.teamscale.jacoco.agent.options.sapnwdi;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.upload.DelayedMultiUploaderBase;
import com.teamscale.jacoco.agent.upload.IUploader;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/**
 * Wraps multiple {@link IUploader}s in order to delay uploads until a {@link CommitDescriptor} is asynchronously made
 * available for each application. Whenever a dump happens the coverage is uploaded to all projects for which a
 * corresponding commit has already been found. Uploads for application that have not commit at that time are skipped.
 * <p>
 * This is safe assuming that the marker class is the central entry point for the application and therefore there should
 * not be any relevant coverage for the application as long as the marker class has not been loaded.
 */
public class DelayedSapNwdiMultiUploader extends DelayedMultiUploaderBase implements IUploader {

	private final BiFunction<CommitDescriptor, SapNwdiApplication, IUploader> uploaderFactory;

	/** The wrapped uploader instances. */
	private final Map<SapNwdiApplication, IUploader> uploaders = new HashMap<>();

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} to test the asynchronous functionality of this
	 * class.
	 */
	public DelayedSapNwdiMultiUploader(
			BiFunction<CommitDescriptor, SapNwdiApplication, IUploader> uploaderFactory) {
		this.uploaderFactory = uploaderFactory;
		registerShutdownHook();
	}

	/** Registers the shutdown hook. */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (getWrappedUploaders().isEmpty()) {
				logger.error("The application was shut down before a commit could be found. The recorded coverage" +
						" is lost.");
			}
		}));
	}

	/** Sets the commit info detected for the application. */
	public void setCommitForApplication(CommitDescriptor commit, SapNwdiApplication application) {
		logger.info("Found commit for " + application.markerClass + ": " + commit);
		IUploader uploader = uploaderFactory.apply(commit, application);
		uploaders.put(application, uploader);
	}

	@Override
	protected Collection<IUploader> getWrappedUploaders() {
		return uploaders.values();
	}
}
