package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.store.IXmlStore;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import org.slf4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Wraps an {@link ICommitDescriptorStore} and an {@link ICachingXmlStore} in order to delay upload to the {@link
 * ICommitDescriptorStore} until a {@link CommitDescriptor} is asynchronously made available. Until then, all XMLs are
 * stored in the {@link ICachingXmlStore}.
 */
public class DelayedCommitDescriptorStore implements IXmlStore {

	private CommitDescriptor commit = null;
	private final Executor executor = Executors.newSingleThreadExecutor();
	private final Logger logger = LoggingUtils.getLogger(this);
	private final ICommitDescriptorStore wrappedStore;
	private final ICachingXmlStore cache;

	public DelayedCommitDescriptorStore(
			ICommitDescriptorStore wrappedStore, ICachingXmlStore cache) {
		this.wrappedStore = wrappedStore;
		this.cache = cache;
		registerShutdownHook(wrappedStore);
	}

	private void registerShutdownHook(ICommitDescriptorStore wrappedStore) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (commit == null) {
				logger.error("The application was shut down before a commit for uploading to {} could be found." +
						" The recorded coverage is lost.", wrappedStore.describe());
			}
		}));
	}

	@Override
	public synchronized void store(String xml) {
		if (commit == null) {
			logger.info("The commit to upload to has not yet been found. Caching coverage XML in {}", cache.describe());
			cache.store(xml);
		} else {
			wrappedStore.store(xml, commit);
		}
	}

	@Override
	public String describe() {
		return "Delayed version of " + wrappedStore.describe() + " caching at " + cache.describe();
	}

	/**
	 * Sets the commit to upload the XMLs to and asynchronously triggers the upload of all cached XMLs. This method
	 * should only be called once.
	 */
	public synchronized void setCommitAndTriggerAsynchronousUpload(CommitDescriptor commit) {
		if (this.commit == null) {
			logger.info("Commit to upload to has been found: {}. Uploading any cached XMLs now to {}", commit,
					wrappedStore.describe());
			this.commit = commit;
			executor.execute(this::uploadCachedXmls);
		} else {
			logger.error("Tried to set upload commit multiple times (old={}, new={}). This is a programming error." +
					" Please report a bug.", this.commit, commit);
		}
	}

	private void uploadCachedXmls() {
		cache.streamCachedXmls().forEach(xml -> wrappedStore.store(xml, commit));
		cache.clear();
		logger.debug("Finished upload of cached XMLs to {}", wrappedStore.describe());
	}
}
