package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.util.InMemoryUploader;
import com.teamscale.report.jacoco.CoverageFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorRetrievalTest {

	@Test
	public void locatorShouldTriggerUploadOfCachedXmls() throws Exception {
		ExecutorService storeExecutor = Executors.newSingleThreadExecutor();
		CoverageFile coverageFile = new CoverageFile(File.createTempFile("jacoco-", ".xml"));
		Path outputPath = coverageFile.getFile().getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(commit -> destination, outputPath,
				storeExecutor);

		ExecutorService locatorExecutor = Executors.newSingleThreadExecutor();
		GitPropertiesLocator locator = new GitPropertiesLocator(store, locatorExecutor);

		store.upload(coverageFile);
		locator.searchJarFileForGitPropertiesAsync(new File(getClass().getResource("git-properties.jar").toURI()));
		locatorExecutor.shutdown();
		locatorExecutor.awaitTermination(5, TimeUnit.SECONDS);
		storeExecutor.shutdown();
		storeExecutor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(Files.list(outputPath).anyMatch(path -> path.getFileName().equals(coverageFile.getFile().toPath())))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}
}