package org.junit.platform.console.tasks;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.console.options.CustomCommandLineOptions;
import org.junit.platform.launcher.Launcher;

public class CustomTestExecutorBase {
	protected final CustomCommandLineOptions options;
	protected final Supplier<Launcher> launcherSupplier;

	public CustomTestExecutorBase(CustomCommandLineOptions options, Supplier<Launcher> launcherSupplier) {
		this.options = options;
		this.launcherSupplier = launcherSupplier;
	}

	protected Optional<ClassLoader> createCustomClassLoader() {
		List<Path> additionalClasspathEntries = options.getAdditionalClasspathEntries();
		if (!additionalClasspathEntries.isEmpty()) {
			URL[] urls = additionalClasspathEntries.stream().map(this::toURL).toArray(URL[]::new);
			ClassLoader parentClassLoader = ClassLoaderUtils.getDefaultClassLoader();
			ClassLoader customClassLoader = URLClassLoader.newInstance(urls, parentClassLoader);
			return Optional.of(customClassLoader);
		}
		return Optional.empty();
	}

	private URL toURL(Path path) {
		try {
			return path.toUri().toURL();
		} catch (Exception ex) {
			throw new JUnitException("Invalid classpath entry: " + path, ex);
		}
	}
}
