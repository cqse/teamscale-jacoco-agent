package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorStoreTest {

	@Test
	public void shouldStoreToCacheIfCommitIsNotKnown() {
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache);

		store.store("xml1");

		assertThat(cache.xmls).containsExactly("xml1");
		assertThat(destination.xmls).isEmpty();
	}

	@Test
	public void shouldStoreToDestinationIfCommitIsKnown() {
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache);

		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		store.store("xml1");

		assertThat(cache.xmls).isEmpty();
		assertThat(destination.xmls).containsExactly("xml1");
	}

	@Test
	public void shouldAsynchronouslyStoreToDestinationOnceCommitIsKnown() throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		InMemoryStore cache = new InMemoryStore();
		InMemoryStore destination = new InMemoryStore();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, cache, executor);

		store.store("xml1");
		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(cache.xmls).isEmpty();
		assertThat(destination.xmls).containsExactly("xml1");
	}

}