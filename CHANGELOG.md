We use [semantic versioning][semver]

# Next release

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
