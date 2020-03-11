package com.teamscale.report.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BashFileSkippingInputStreamTest {

	@Test
	void testBashFileJar() throws IOException {
		ArrayList<String> filesInJar = getEntriesFromJarFile("spring-boot-executable-example.jar");
		assertThat(filesInJar).hasSize(110);
	}

	@Test
	void testNormalJar() throws IOException {
		ArrayList<String> filesInJar = getEntriesFromJarFile("normal.jar");
		assertThat(filesInJar).hasSize(284);
	}

	private ArrayList<String> getEntriesFromJarFile(String resourceName) throws IOException {
		InputStream inputStream = getClass().getResourceAsStream(resourceName);
		BashFileSkippingInputStream bashFileSkippingInputStream = new BashFileSkippingInputStream(inputStream);
		JarInputStream jarInputStream = new JarInputStream(bashFileSkippingInputStream);
		JarEntry entry;
		ArrayList<String> filesInJar = new ArrayList<>();
		while ((entry = jarInputStream.getNextJarEntry()) != null) {
			filesInJar.add(entry.getName());
		}
		return filesInJar;
	}
}
