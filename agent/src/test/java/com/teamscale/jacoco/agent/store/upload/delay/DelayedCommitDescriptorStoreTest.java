package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

	private static class InMemoryStore implements ICachingXmlStore {

		private final List<String> xmls = new ArrayList<>();

		@Override
		public Stream<String> streamCachedXmls() {
			return xmls.stream();
		}

		@Override
		public void clear() {
			xmls.clear();
		}

		@Override
		public void store(String xml) {
			xmls.add(xml);
		}

		@Override
		public String describe() {
			return "in memory";
		}
	}
}