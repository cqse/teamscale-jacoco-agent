package com.teamscale.client;

/** Information about the profiler including the process it is attached to as well as the configuration it is running with. */
public class ProfilerInfo {

	/** Information about the machine and process the profiler is running on. */
	public ProcessInformation processInformation;

	/** Concrete config that the profiler is running with. */
	public ProfilerConfiguration profilerConfiguration;

	public ProfilerInfo(ProcessInformation processInformation, ProfilerConfiguration profilerConfiguration) {
		this.processInformation = processInformation;
		this.profilerConfiguration = profilerConfiguration;
	}
}
