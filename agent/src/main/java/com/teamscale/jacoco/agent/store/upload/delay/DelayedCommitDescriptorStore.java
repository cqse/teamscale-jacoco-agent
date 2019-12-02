package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.store.IXmlStore;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.slf4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Wraps an {@link IXmlStore} and an {@link ICachingXmlStore} in order to delay upload to the {@link IXmlStore} until a
 * {@link CommitDescriptor} is asynchronously made available. Until then, all XMLs are stored in the {@link
 * ICachingXmlStore}.
 */
public class DelayedCommitDescriptorStore implements IXmlStore {

	private final Executor executor;
	private final Logger logger = LoggingUtils.getLogger(this);
	private final Function<CommitDescriptor, IXmlStore> wrappedStoreFactory;
	private IXmlStore wrappedStore = null;
	private final ICachingXmlStore cache;

	public DelayedCommitDescriptorStore(Function<CommitDescriptor, IXmlStore> wrappedStoreFactory,
										ICachingXmlStore cache) {
		this(wrappedStoreFactory, cache, Executors.newSingleThreadExecutor(
				new DaemonThreadFactory(DelayedCommitDescriptorStore.class, "Delayed store cache upload thread")));
	}

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} to test the asynchronous functionality of this
	 * class.
	 */
	/*package*/ DelayedCommitDescriptorStore(Function<CommitDescriptor, IXmlStore> wrappedStoreFactory,
											 ICachingXmlStore cache, Executor executor) {
		this.wrappedStoreFactory = wrappedStoreFactory;
		this.cache = cache;
		this.executor = executor;

		registerShutdownHook();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (wrappedStore == null) {
				logger.error("The application was shut down before a commit could be found. The recorded coverage" +
								" is still cached in {} but will not be automatically processed. You configured the" +
								" agent to auto-detect the commit to which the recorded coverage should be uploaded to" +
								" Teamscale. In order to fix this problem, you need to provide a git.properties file" +
								" in all of the profiled Jar/War/Ear/... files. If you're using Gradle or" +
								" Maven, you can use a plugin to create a proper git.properties file for you, see" +
								" https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto-git-info",
						cache.describe());
			}
		}));
	}

	@Override
	public synchronized void store(String xml) {
		if (wrappedStore == null) {
			logger.info("The commit to upload to has not yet been found. Caching coverage XML in {}", cache.describe());
			cache.store(xml);
		} else {
			wrappedStore.store(xml);
		}
	}

	@Override
	public String describe() {
		if (wrappedStore != null) {
			return wrappedStore.describe();
		}
		return "Temporary cache until commit is resolved: " + cache.describe();
	}

	/**
	 * Sets the commit to upload the XMLs to and asynchronously triggers the upload of all cached XMLs. This method
	 * should only be called once.
	 */
	public synchronized void setCommitAndTriggerAsynchronousUpload(CommitDescriptor commit) {
		if (wrappedStore == null) {
			wrappedStore = wrappedStoreFactory.apply(commit);
			logger.info("Commit to upload to has been found: {}. Uploading any cached XMLs now to {}", commit,
					wrappedStore.describe());
			executor.execute(this::uploadCachedXmls);
		} else {
			logger.error("Tried to set upload commit multiple times (old store: {}, new commit: {})." +
					" This is a programming error. Please report a bug.", wrappedStore.describe(), commit);
		}
	}

	private void uploadCachedXmls() {
		cache.streamCachedXmls().forEach(xml -> wrappedStore.store(xml));
		cache.clear();
		logger.debug("Finished upload of cached XMLs to {}", wrappedStore.describe());
	}
}
