package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.sapnwdi.NwdiConfiguration;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wraps an {@link IUploader} and in order to delay upload until a {@link CommitDescriptor} is asynchronously made
 * available.
 */
public class DelayedNwdiUploader implements IUploader {

	private final Logger logger = LoggingUtils.getLogger(this);
	private final BiFunction<CommitDescriptor, NwdiConfiguration.NwdiApplication, IUploader> wrappedUploaderFactory;
	private final Map<NwdiConfiguration.NwdiApplication, IUploader> wrappedUploaders = new HashMap<>();

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} to test the asynchronous functionality of this
	 * class.
	 */
	public DelayedNwdiUploader(
			BiFunction<CommitDescriptor, NwdiConfiguration.NwdiApplication, IUploader> wrappedUploaderFactory) {
		this.wrappedUploaderFactory = wrappedUploaderFactory;
		registerShutdownHook();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (wrappedUploaders.isEmpty()) {
				logger.error("The application was shut down before a commit could be found. The recorded coverage" +
						" is lost. You configured the agent to auto-detect the commit via the " +
						"'Implementation-Version' entry in the MANIFEST.MF to which the recorded " +
						"coverage should be uploaded to Teamscale.");
			}
		}));
	}

	@Override
	public synchronized void upload(CoverageFile file) {
		if (wrappedUploaders.isEmpty()) {
			logger.info("The commit to upload to has not yet been found. Discarding coverage");
		} else {
			for (IUploader wrappedUploader : wrappedUploaders.values()) {
				wrappedUploader.upload(file.acquireReference());
			}
		}
	}

	@Override
	public String describe() {
		if (!wrappedUploaders.isEmpty()) {
			return wrappedUploaders.values().stream().map(IUploader::describe).collect(Collectors.joining(", "));
		}
		return "Temporary stand-in until commit is resolved";
	}

	public void setCommitForApplication(CommitDescriptor commit, NwdiConfiguration.NwdiApplication application) {
		application.setFoundTimestamp(commit);
		IUploader wrappedUploader = wrappedUploaderFactory.apply(commit, application);
		wrappedUploaders.put(application, wrappedUploader);
	}
}
