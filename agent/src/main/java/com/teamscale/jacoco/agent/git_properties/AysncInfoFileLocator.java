package com.teamscale.jacoco.agent.git_properties;

import com.teamscale.jacoco.agent.upload.delay.DelayedCommitDescriptorUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AysncInfoFileLocator {

	protected final Executor executor;

	protected final DelayedCommitDescriptorUploader store;

	public AysncInfoFileLocator(DelayedCommitDescriptorUploader store) {
		// using a single threaded executor allows this class to be lock-free
		this(store, Executors
				.newSingleThreadExecutor(
						new DaemonThreadFactory(GitPropertiesLocator.class, "Async Jar scanner thread")));
	}

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} in order to test the asynchronous functionality
	 * of this class.
	 */
	public AysncInfoFileLocator(DelayedCommitDescriptorUploader store, Executor executor) {
		this.store = store;
		this.executor = executor;
	}

	/** Asynchronously searches the given jar file for a git.properties file. */
	public void searchJarFileAsync(File jarFile) {
		executor.execute(() -> searchJarFile(jarFile));
	}


	protected abstract void searchJarFile(File jarFile);
}
