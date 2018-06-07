# Teamscale JaCoCo Agent

## Downloading

We use Gitlab CI to build the distribution zip. Any successful build will have it in its
build artifacts under `build/distributions`. Download it from there.

In case Gitlab has pruned the artifacts (happens after 1 week) you can simply re-run the
build.

__Use only zips produced by builds for a version tag (Tags tab in Gitlab). This ensures
that we always know which exact version a customer has deployed.__

## Publishing

You will need to install pandoc and have it on the path.

- Update the changelog
- Increase the version number in the build file. We use [semantic versioning](http://semver.org)!
- Create a git tag.
- Push the tag and your revisions to git

   git push
   git push --tags

- The build server will build the project and you can download a build artifact with the dist afterwards

## Compiling for a different JaCoCo version

- change `ext.jacocoVersion` in the build script
- `gradlew dist`
- **Do not commit unless you are upgrading to a newer version!**

