package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.utils.InMemoryStore;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorRetrievalTest {

	@Test
	public void locatorShouldTriggerUploadOfCachedXmls() throws Exception {
		ExecutorService storeExecutor = Executors.newSingleThreadExecutor();
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache,
				storeExecutor);

		ExecutorService locatorExecutor = Executors.newSingleThreadExecutor();
		GitPropertiesLocator locator = new GitPropertiesLocator(store, locatorExecutor);

		store.store("xml1");
		locator.searchJarFileForGitPropertiesAsync(new File(getClass().getResource("git-properties.jar").toURI()));
		locatorExecutor.shutdown();
		locatorExecutor.awaitTermination(5, TimeUnit.SECONDS);
		storeExecutor.shutdown();
		storeExecutor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(cache.getXmls()).isEmpty();
		assertThat(destination.getXmls()).containsExactly("xml1");
	}

}