package com.teamscale.jacoco.agent.sapnwdi;

/** Thrown in case an NWDI Manifest file cannot be read. */
public class NwdiManifestException extends Exception {
	/*package*/ NwdiManifestException(String s, Throwable throwable) {
		super(s, throwable);
	}
	/*package*/ NwdiManifestException(String s) {
		super(s);
	}
}
