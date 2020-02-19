package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.util.InMemoryUploader;
import com.teamscale.jacoco.agent.util.TmpUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorStoreTest {

	@Test
	public void shouldStoreToCacheIfCommitIsNotKnown() throws IOException {
		File coverageFile = File.createTempFile("jacoco-", ".xml");
		Path outputPath = coverageFile.getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, outputPath);

		store.upload(coverageFile);

		System.out.println(coverageFile.getName());
		assertThat(
				Files.list(outputPath).anyMatch(path -> path.getFileName().equals(Paths.get(coverageFile.getName()))))
				.isTrue();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isFalse();
	}

	@Test
	public void shouldStoreToDestinationIfCommitIsKnown() throws IOException {
		File coverageFile = File.createTempFile("jacoco-", ".xml");
		Path outputPath = coverageFile.getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, outputPath);

		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		store.upload(coverageFile);

		System.out.println(coverageFile.getName());
		assertThat(
				Files.list(outputPath).anyMatch(path -> path.getFileName().equals(Paths.get(coverageFile.getName()))))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}

	@Test
	public void shouldAsynchronouslyStoreToDestinationOnceCommitIsKnown() throws Exception {
		File coverageFile = File.createTempFile("jacoco-", ".xml");
		Path outputPath = coverageFile.getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, outputPath,
				executor);

		store.upload(coverageFile);
		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		System.out.println(coverageFile.getName());
		assertThat(
				Files.list(outputPath).anyMatch(path -> path.getFileName().equals(Paths.get(coverageFile.getName()))))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}

	// TODO teardown to delete coverage files
	@AfterAll
	static void teardown() throws IOException {
		TmpUtils.cleanTmpFolder();
	}
}