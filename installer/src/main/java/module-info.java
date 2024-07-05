module com.teamscale.profiler.installer {
	requires okhttp3;
	requires info.picocli;
	requires com.sun.jna.platform;
	requires org.apache.commons.lang3;
	requires org.apache.commons.io;
	// adds support for elliptic curve SSL ciphers
	// used e.g. by teamscale.io
	requires jdk.crypto.ec;
	exports com.teamscale.profiler.installer;
	opens com.teamscale.profiler.installer;
	exports com.teamscale.profiler.installer.utils;
	exports com.teamscale.profiler.installer.windows;
	opens com.teamscale.profiler.installer.utils;
}

