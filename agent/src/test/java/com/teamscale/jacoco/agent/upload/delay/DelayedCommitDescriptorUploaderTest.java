package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.jacoco.agent.util.InMemoryUploader;
import com.teamscale.report.jacoco.CoverageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorUploaderTest {

	@Test
	public void shouldStoreToCacheIfCommitIsNotKnown(@TempDir Path outputPath) throws IOException {
		Path coverageFilePath = outputPath
				.resolve(String.format("jacoco-%d.xml", ZonedDateTime.now().toInstant().toEpochMilli()));
		CoverageFile coverageFile = new CoverageFile(Files.createFile(coverageFilePath).toFile());

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath);

		store.upload(coverageFile);

		assertThat(Files.list(outputPath).collect(Collectors.toList()))
				.contains(coverageFilePath);
		assertThat(destination.getUploadedFiles()).doesNotContain(coverageFile);
	}

	@Test
	public void shouldStoreToDestinationIfCommitIsKnown(@TempDir Path outputPath) throws IOException {
		Path coverageFilePath = outputPath
				.resolve(String.format("jacoco-%d.xml", ZonedDateTime.now().toInstant().toEpochMilli()));
		CoverageFile coverageFile = new CoverageFile(Files.createFile(coverageFilePath).toFile());

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath);

		store.setCommitAndTriggerAsynchronousUpload("a2afb54566aaa");
		store.upload(coverageFile);

		assertThat(Files.list(outputPath).collect(Collectors.toList()))
				.doesNotContain(coverageFilePath);
		assertThat(destination.getUploadedFiles()).contains(coverageFile);
	}

	@Test
	public void shouldAsynchronouslyStoreToDestinationOnceCommitIsKnown(@TempDir Path outputPath) throws Exception {
		Path coverageFilePath = outputPath
				.resolve(String.format("jacoco-%d.xml", ZonedDateTime.now().toInstant().toEpochMilli()));
		CoverageFile coverageFile = new CoverageFile(Files.createFile(coverageFilePath).toFile());

		InMemoryUploader destination = new InMemoryUploader();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath,
				executor);

		store.upload(coverageFile);
		store.setCommitAndTriggerAsynchronousUpload("a2afb54566aaa");
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(Files.list(outputPath).collect(Collectors.toList()))
				.doesNotContain(coverageFilePath);
		assertThat(destination.getUploadedFiles()).contains(coverageFile);
	}
}