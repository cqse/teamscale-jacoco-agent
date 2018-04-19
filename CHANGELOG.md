We use [semantic versioning][semver]

# Next release

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

- [feature] added `convert` mode to do one-time conversion. The old behaviour is now reachable via the `watch` command (which is also the default if no command is provided).

# 1.4.1

- [tooling] add GitLab build and `dist` task. Update documentation

# 1.4.0

- [feature] added `-d` switch to ignore non-identical, duplicate class files during report generation


[semver]: http://semver.org/
