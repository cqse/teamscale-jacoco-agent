package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.jacoco.agent.commit_resolution.git_properties.CommitInfo;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatorUtils;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitSingleProjectPropertiesLocator;
import com.teamscale.jacoco.agent.util.InMemoryUploader;
import com.teamscale.report.jacoco.CoverageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCommitDescriptorRetrievalTest {

	@Test
	public void locatorShouldTriggerUploadOfCachedXmls(@TempDir Path outputPath) throws Exception {
		ExecutorService storeExecutor = Executors.newSingleThreadExecutor();
		Path coverageFilePath = outputPath
				.resolve(String.format("jacoco-%d.xml", ZonedDateTime.now().toInstant().toEpochMilli()));
		CoverageFile coverageFile = new CoverageFile(Files.createFile(coverageFilePath).toFile());

		InMemoryUploader destination = new InMemoryUploader();
		DelayedUploader<CommitInfo> store = new DelayedUploader<>(commit -> destination, outputPath,
				storeExecutor);

		ExecutorService locatorExecutor = Executors.newSingleThreadExecutor();
		GitSingleProjectPropertiesLocator<CommitInfo> locator = new GitSingleProjectPropertiesLocator<>(store,
				GitPropertiesLocatorUtils::getCommitInfoFromGitProperties, locatorExecutor, true);

		store.upload(coverageFile);
		locator.searchFileForGitPropertiesAsync(new File(getClass().getResource("git-properties.jar").toURI()), true);
		locatorExecutor.shutdown();
		locatorExecutor.awaitTermination(5, TimeUnit.SECONDS);
		storeExecutor.shutdown();
		storeExecutor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(Files.list(outputPath)
				.anyMatch(path -> path.equals(coverageFilePath)))
				.isFalse();
		assertThat(destination.getUploadedFiles().contains(coverageFile)).isTrue();
	}
}