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
		InputStream inputStream = getClass().getResourceAsStream("spring-boot-executable-example.jar");
		BashFileSkippingInputStream bashFileSkippingInputStream = new BashFileSkippingInputStream(inputStream);
		JarInputStream jarInputStream = new JarInputStream(bashFileSkippingInputStream);
		JarEntry entry;
		ArrayList<String> filesInJar = new ArrayList<>();
		while ((entry = jarInputStream.getNextJarEntry()) != null) {
			filesInJar.add(entry.getName());
		}
		assertThat(filesInJar).hasSize(110);
	}
}
