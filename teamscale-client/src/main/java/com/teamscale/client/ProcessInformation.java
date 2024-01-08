package com.teamscale.client;

/** Information about the process and machine the profiler is attached to. */
public class ProcessInformation {

	/** Hostname of the machine it is running on */
	public final String hostname;

	/** Profiled PID */
	public final String pid;

	/** The timestamp at which the process was started. */
	public final long startedAtTimestamp;

	public ProcessInformation(String hostname, String pid, long startedAtTimestamp) {
		this.hostname = hostname;
		this.pid = pid;
		this.startedAtTimestamp = startedAtTimestamp;
	}
}
