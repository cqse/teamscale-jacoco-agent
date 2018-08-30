# Teamscale JaCoCo Agent [![Build Status](https://travis-ci.com/cqse/teamscale-jacoco-agent.svg?branch=master)](https://travis-ci.com/cqse/teamscale-jacoco-agent)

## Download

* [Binary Distribution](https://github.com/cqse/teamscale-jacoco-agent/releases)
* [Docker Container](https://hub.docker.com/r/cqse/teamscale-jacoco-agent/tags/)

## Documentation

* [Teamscale JaCoCo Agent](agent/USAGE.md)
* [Java Web Start Wrapper](javaws-wrapper/README.md)

## Development

### Contributing

* Create a GitHub issue for changes
* Use pull requests. Complete the "Definition of Done" for every pull request.
* There's a Teamscale project, please fix all findings before submitting your pull request for review. The Teamscale coding guidelines and Definition of Done apply as far as possible with the available tooling.

### Publishing

After merging, please create a GitHub Release tag with the version number, e.g. `v8.1.0`
All tags are built automatically using [Travis CI](https://travis-ci.com/cqse/teamscale-jacoco-agent) and [Docker Cloud Build](https://cloud.docker.com/swarm/cqse/repository/docker/cqse/teamscale-jacoco-client) with the release binaries being uploaded to the GitHub Releases.

Only use releases from tagged commits. This ensures that we always know which code is running in production.

### Compiling for a different JaCoCo version

* change `ext.jacocoVersion` in the build script
* `gradlew dist`
* **Do not commit unless you are upgrading to a newer version!**
