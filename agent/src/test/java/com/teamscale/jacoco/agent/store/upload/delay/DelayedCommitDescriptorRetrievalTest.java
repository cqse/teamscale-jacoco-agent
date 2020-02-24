package com.teamscale.jacoco.agent.store.upload.delay;

import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.util.InMemoryUploader;
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
		File coverageFile = File.createTempFile("jacoco-", ".xml");
		Path outputPath = coverageFile.getParentFile().toPath();

		InMemoryUploader destination = new InMemoryUploader();
		DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(commit -> destination, outputPath,
				storeExecutor);

		ExecutorService locatorExecutor = Executors.newSingleThreadExecutor();
		GitPropertiesLocator locator = new GitPropertiesLocator(store, locatorExecutor);

		store.upload(coverageFile);
		locator.searchJarFileForGitPropertiesAsync(new File(getClass().getResource("git-properties.jar").toURI()));
		locatorExecutor.shutdown();
		locatorExecutor.awaitTermination(5, TimeUnit.SECONDS);
		storeExecutor.shutdown();
		storeExecutor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(Files.list(outputPath).anyMatch(path -> path.getFileName().equals(coverageFile.toPath())))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}
}