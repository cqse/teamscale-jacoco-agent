package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.TestBase;
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

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorUploaderTest extends TestBase {

	@Test
	public void shouldStoreToCacheIfCommitIsNotKnown() throws IOException {
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getFile().getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath);

		store.upload(coverageFile);

		assertThat(
				Files.list(outputPath)
						.anyMatch(path -> path.getFileName().equals(Paths.get(coverageFile.getFile().getName()))))
				.isTrue();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isFalse();
	}

	@Test
	public void shouldStoreToDestinationIfCommitIsKnown() throws IOException {
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getFile().getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath);

		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		store.upload(coverageFile);

		assertThat(
				Files.list(outputPath)
						.anyMatch(path -> path.getFileName().equals(Paths.get(coverageFile.getFile().getName()))))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}

	@Test
	public void shouldAsynchronouslyStoreToDestinationOnceCommitIsKnown() throws Exception {
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getFile().getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath,
				executor);

		store.upload(coverageFile);
		store.setCommitAndTriggerAsynchronousUpload(new CommitDescriptor("branch", 1234));
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(
				Files.list(outputPath)
						.anyMatch(path -> path.getFileName().equals(Paths.get(coverageFile.getFile().getName()))))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}
}