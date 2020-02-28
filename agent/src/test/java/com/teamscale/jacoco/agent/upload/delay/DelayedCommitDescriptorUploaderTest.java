package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.util.InMemoryUploader;
import com.teamscale.report.jacoco.CoverageFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorUploaderTest {

	@Test
	public void shouldStoreToCacheIfCommitIsNotKnown() throws IOException {
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getParentDirectoryPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath);

		store.upload(coverageFile);

		assertThat(Files.list(outputPath).collect(Collectors.toList()))
				.contains(Paths.get(coverageFile.getAbsolutePath()));
		assertThat(destination.getUploadedFiles()).doesNotContain(coverageFile);
	}

	@Test
	public void shouldStoreToDestinationIfCommitIsKnown() throws IOException {
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getParentDirectoryPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath);

		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		store.upload(coverageFile);

		assertThat(Files.list(outputPath).collect(Collectors.toList()))
				.doesNotContain(Paths.get(coverageFile.getAbsolutePath()));
		assertThat(destination.getUploadedFiles()).contains(coverageFile);
	}

	@Test
	public void shouldAsynchronouslyStoreToDestinationOnceCommitIsKnown() throws Exception {
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getParentDirectoryPath();

		InMemoryUploader destination = new InMemoryUploader();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath,
				executor);

		store.upload(coverageFile);
		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(Files.list(outputPath).collect(Collectors.toList()))
				.doesNotContain(Paths.get(coverageFile.getAbsolutePath()));
		assertThat(destination.getUploadedFiles()).contains(coverageFile);
	}
}