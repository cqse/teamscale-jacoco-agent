We use [semantic versioning][semver]

# Next version

# 11.1.0
- [feature] The agent now also supports line coverage in Testwise Coverage mode

# 11.0.4
- [breaking change] Test impact mode no longer uploads reports to teamscale and does no longer generate reports on its own (see TEST_IMPACT_ANALYSIS_DOC -> How to get testwise coverage)
- [feature] Added `test-env` option

# 10.2.0
- [feature] Added option to upload to azure file storage

# 10.1.0
- [feature] Paths passed to the agent can now be relative and contain ant patterns

# 10.0.0
- [breaking change] switched to logback for logging. All logging configurations must be replaced
  with logback XMLs. This fixes Java 10 compatibility issues with Log4j by removing Log4j from
  the agent
- [feature] make agent log INFO and above to `agentdir/logs` by default
- [fix] isolate agent further from the application to prevent errors from conflicting library versions

# 9.1.0
- [feature] dump interval of 0 only dumps at the end

# 9.0.3
- [fix] Test Impact Mode: Empty reports are no longer dumped
- [fix] Test Impact Mode: JUnit is uploaded before testwise coverage

# 9.0.2
- [fix] prevent log files from flooding disk with default log4j config files

# 9.0.1
- [fix] NativeWebSocketServletContainerInitializer not found

# 9.0.0
- [breaking change] added `teamscale-commit-manifest-jar` option. Automatic fallback to MANIFEST.MF on classpath is no longer supported

# 8.4.0
- [feature] added `config-file` option

# 8.3.1
- [fix] fixed class conflict with JAXB in WebSphere

# 8.3.0
- [feature] added Test Impact mode via `http-server-port`

# 8.2.0
- [feature] added ability to read commit branch and timestamp from MANIFEST.MF

# 8.1.0
- [feature] added options to configure coverage upload to Teamscale

# 8.0.2
- [fix] remove version number from jar file name

# 8.0.1
- [fix] prefill the `javaws.properties` correctly on Windows

# 8.0.0
- [breaking change] removed `watch` mode and made `convert` mode the default. Only the agent is now supported

# 7.0.0
- [feature] prevent clashes between our dependencies and the profile app's
- [breaking change] logging for the agent must now be configured via the `logging-config` parameter

# 6.0.0
- [breaking change] Docker image is now designed to use the agent instead of the client
- [feature] add wrapper around `javaws` to allow profiling Java Web Start applications

# 5.0.0
- [breaking change] ignore-duplicates is now the default for the agent to simplify the initial setup

# 4.0.3
- [fix] fix handling of Windows paths

# 4.0.2
- [fix] using `upload-url` in conjunction with `upload-metadata` caused a crash

# 4.0.1
_This version was never checked in, thus there is no tag for it in the Git_
- [fix] using `upload-url` in conjunction with `upload-metadata` caused corrupt files

# 4.0.0
- [breaking change] agent option separator is now a semicolon, no longer a colon (to support Windows paths)
- [breaking change] merged `include` and `jacoco-include` parameters. Both use the JaCoCo pattern syntax
- [feature] allow dumping via HTTP file upload

# 3.1.0
- [feature] add agent for regular dumping without separate process

# 3.0.0
- [breaking change] you must pass `true` or `false` explicitly to the `-d` parameter
- [feature] allow setting explicit host name via `--host`
- [feature] add Docker image
- [tooling] add Docker image build and Docker Hub project

# 2.0.1
- [fix] working directory was missing from systemd service file

# 2.0.0

- [feature] added `convert` mode to do one-time conversion. The old behavior is now reachable via the `watch` command (which is also the default if no command is provided).

# 1.4.1

- [tooling] add GitLab build and `dist` task. Update documentation

# 1.4.0

- [feature] added `-d` switch to ignore non-identical, duplicate class files during report generation


[semver]: http://semver.org/
