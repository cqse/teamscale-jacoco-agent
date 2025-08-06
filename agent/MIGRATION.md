# Migration Guide to v33.0.0

With the release 33.0.0 a new installer was introduced that simplifies the setup for new users and for managing multiple profilers on one machine.
You don't need to change anything in your existing setups.
The old configuration approaches are still fully supported.

The new recommended approach is to store the profiler configuration within Teamscale.
This provides a central place to manage profiler configurations across machines and get an overview over currently running profilers (see [Coverage Profilers View Documentation](https://docs.teamscale.com/reference/ui/project/coverage-profilers/)).  

If you want to switch to use the new installer-based setup (Linux only for now), run the following command:
```sh
./installer install https://your.teamscale.url your-build-user-name your-access-token
```

This will install the agent to `/opt/teamscale-profiler/java` and will attach it to all Java applications.

Then for each application where you currently use the Teamscale Java Profiler: 
- Create a profiler configuration within Teamscale with the same options that you previously specified via the `config-file=agent.properties`
  - Omit the `teamscale-server-url`, `teamscale-user` and `teamscale-access-key` options
- Instead, set the `TEAMSCALE_JAVA_PROFILER_CONFIG_ID=my-configuration-id` environment variable for the Java process that runs your system under test (e.g., the application server or systemd service).
- Remove the existing configuration that manually attaches the Teamscale Java Profiler to the System under Test  
- Restart the application and verify that the profiler is shown in Teamscale under `Running Profilers`
- Perform some actions in the System under Test and shut it down
- Verify that new coverage was uploaded to Teamscale
