# Teamscale JaCoCo Client

## Publishing

You will need to install pandoc and have it on the path.

- Update the changelog
- Increase the version number in the build file. We use [semantic versioning](http://semver.org)!
- Create a git tag.
- Build the distribution: `gradlew dist`

## Compiling for a different JaCoCo version

- change `ext.jacocoVersion` in the build script
- `gradlew dist`
- **Do not commit unless you are upgrading to a newer version!**

