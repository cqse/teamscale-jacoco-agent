We use [semantic versioning](http://semver.org/):

- MAJOR version when you make incompatible API changes,
- MINOR version when you add functionality in a backwards-compatible manner, and
- PATCH version when you make backwards compatible bug fixes.

# Next Release
- [fix] Upload to teamscale mode for testwise coverage did report tests multiple times when multiple uploads were triggered
- [fix] Setting partition or message via REST did not strip JSON encoding quotes

# 21.5.0
- [feature] Support for VFS (Virtual File System) URL format used in JBoss and Wildfly

# 21.4.0
- [feature] _tia-client_: dynamically set partition and message
- [fix] Upload to teamscale mode for testwise coverage did miss some class files when class-dir was not explicitly given

# 21.3.0
- [feature] _tia-client_: expose rank of tests

# 21.2.1
- [fix] Spring Boot applications could not be profiled due to changes in their code location format

# 21.2.0
- [feature] _teamscale-client_: Impacted tests now allows to specify multiple partitions
- [feature] _teamscale-client_: Impacted tests baseline can now be a string
- [feature] _teamscale-client_: Impacted tests are queried with CHEAP_ADDITIONAL_COVERAGE_PER_TIME

# 21.1.0

- [feature] Support uploading to multiple Teamscale projects

# 21.0.0
- [feature] _tia-client_: add API to hash test data when creating ClusteredTestDetails
- [feature] support for SAP NWDI application profiling
- [feature] _teamscale-gradle-plugin_: support jacoco and junit report uploads to Teamscale
- [feature] _teamscale-gradle-plugin_: available on Gradle Plugin Portal
- [breaking change] _teamscale-gradle-plugin_: teamscale.report.partition is no longer available

# 20.0.0
- [breaking change] This release requires Teamscale 5.9 or higher
- [feature] Made ensure-processed and include-failed-and-skipped options available in teamscale-client

# 19.0.0
- [fix] option parsing errors were not logged in rare cases
- [breaking change] changed default message to include partition and upload date
- [breaking change] Bumped minimum supported Gradle version for the Gradle plugin to 6.5 so we can support Java 11
- [feature] Made score and duration of test selected by TIA available via the teamscale-client

# 18.0.0
- [breaking change] removed options `coverage-via-http` and `teamscale-testwise-upload`. Use `tia-mode=http` or `tia-mode=teamscale-upload` instead.
- [breaking change] _tia-client_: changed Java API
- [feature] always exclude common libraries from profiling to shrink coverage files
- [feature] enable changing and reading the commit message via a new `/message` REST endpoint
- [breaking change] changing the partition is now a PUT request to `/partition` (formerly POST to `/partition/{partitionName}`)

# 17.0.0
- [breaking change] default for `--interval` changed from 60 to 480 minutes
- [feature] Support for uploading XML reports to artifactory
- [fix] Docker image did not react to SIGTERM
- [feature] All artifacts are now also available on Maven Central
- [fix] `test-env` option was ignored

# 16.0.1
- [fix] revision is ignored during validation for automatic upload to Teamscale

# 16.0.0
- [fix] Prevent "out of memory" in small JVMs: Don't cache test executions in memory
- [breaking change] `--ignore-duplicates` (and `-d` option in convert tool) have been replaced with `--duplicates` option
- [breaking change] `--filter` option in convert tool has been renamed to `--includes`
- [breaking change] `--exclude` option in convert tool has been renamed to `--excludes`
- [fix] Remove retry logic for impacted tests request
- [fix] Resolve possible memory leak during report generation
- [feature] Use git.commit.id from git.properties instead of branch and timestamp
- [breaking change] Reduce XML report size by only including source file coverage, no class coverage
- [feature] New option `--ignore-uncovered-classes` to further reduce size of XML reports
- [fix] converter produces duplicate test entries for testwise coverage
- [breaking change] `--classDir` option in convert tool has been renamed to `--class-dir`
- [feature] `--class-dir` option allows to pass in a `.txt` file with the class file directories/jars separated by newlines

# 15.5.0
- [feature] add TIA client library for integrating TIA in your custom test framework
- [fix] Delete empty coverage directories
- [fix] Don't upload empty coverage reports

# 15.4.0
- [fix] `git.properties` commit was not used for upload
- [feature] Upload ignored `origin/` prefix in `git.properties`'s branch name
- [fix] `http-server-port` option does not pass validation in normal mode
- [fix] Executable spring boot jar produces no coverage

# 15.3.0
- [feature] Added `coverage-via-http` option for testwise mode
- [feature] Added `teamscale-revision` option to supply VCS revision instead of branch and timestamp
- [feature] Added `/revision` HTTP endpoint for testwise mode
- [feature] Updated JaCoCo to 0.8.5
- [fix] Significantly reduced memory footprint
- [fix] `--run-all-tests` doesn't run any tests at all
- [fix] test-wise coverage report incorrect for classes in default package

# 15.2.0
- [fix] WildFly won't start with agent attached
- [feature] make `out` parameter optional with sensible fallback (subdirectory `coverage` in agent installation directory)
- [feature] if no `teamscale-commit`, `teamscale-git-properties-jar` or `teamscale-commit-manifest-jar` is configured,
  all loaded Jar/War/Ear/... files that contain profiled code are checked for a `git.properties` file. This allows auto-detecting a
  `git.properties` file without any additional configuration. 

# 15.1.1
- [documentation] Configuration for SAP NetWeaver Java (>= 7.50) is now documented

# 15.1.0
- [feature] supplying a `class-dir` option is no longer mandatory
- [feature] agent logs errors in case of empty coverage dumps (i.e. misconfigured agent)
- [fix] prevent NPE when trying to read manifest from Jar file

# 15.0.0
- [feature] support for git.properties to supply commit information
- [breaking change] Agent now ignores SSL certificates by default and only turns on validation if
  `validate-ssl=true` is passed in the agent arguments. Existing setups will continue to work but
  validation will be disabled from this version on
- [fix] Agent uses higher timeouts (20s) for all HTTP connections to account for slow networks

# 14.0.0
- [fix] Reduced memory requirements for generating testwise coverage.
- [breaking change] When using the `convert` tool in `--testwise-coverage` mode the new `--split-after 5000` option will
  break up testwise coverage files automatically after the specified number of tests written. This ensures that generated 
  reports are small enough to be uploaded to Teamscale. Default is `5000`. The given output file will now be appended 
  suffixed `-1`, `-2` etc.. If the specified output file is named `testwise_coverage.json` the actually written file 
  will be called `testwise_coverage-1.json` for the first 5000 tests. For uploading multiple files use an upload session.

# 13.0.1
- [fix] Prevent `-1` to show up as covered line in Testwise Coverage report

# 13.0.0
- [feature] added `dump-on-exit` option
- [breaking change] added `mode` option (Must be set for Testwise Coverage mode. `http-server-port` or `test-env` alone is no longer sufficient)
- [feature] The agent now optionally accepts test execution results via /test/end
- [feature] Support for Java 12

# 12.0.0
- [breaking change] The convert tool now uses wildcard patterns for the class matching (was ant pattern before)
- [breaking change] The agent returns the correct 204 and 400 status codes in Testwise Coverage mode

# 11.2.0
- [feature] The agent now also supports Java 11 code.

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
