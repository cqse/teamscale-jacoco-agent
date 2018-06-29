# Teamscale JaCoCo Agent

## Contributing

- Please create a GitLab issue for all changes
- Use merge requests. Use the "Definition of Done" description template for every merge request.
- There's a Teamscale project, please fix all findings before submitting your
  merge request for review. The Teamscale coding guidelines and Definition of Done
  apply as far as possible with the available tooling.
- After merging, please tag the merge commit with the version number, e.g. `v8.1.0`

## Downloading

We use Gitlab CI to build the distribution zip. Any successful build will have it in its
build artifacts under `build/distributions`. Download it from there.

In case Gitlab has pruned the artifacts (happens after 1 week) you can simply re-run the
build.

__Use only zips produced by builds for a version tag (Tags tab in Gitlab). This ensures
that we always know which exact version a customer has deployed.__

## Publishing

All tags are built automatically. Only use builds from tagged commits. This ensures that
we always know which code a customer is using.

The build server will build the project and you can download a build artifact with the dist afterwards.
If the artifcats have been removed already, trigger the build again in GitLab.

## Compiling for a different JaCoCo version

- change `ext.jacocoVersion` in the build script
- `gradlew dist`
- **Do not commit unless you are upgrading to a newer version!**
- When upgrading to a newer version make sure to check the comment in the AnalyzerCache class.
