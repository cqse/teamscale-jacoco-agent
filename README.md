# Teamscale JaCoCo Client

## Compiling for a different JaCoCo version

- change `ext.jacocoVersion` in the build script
- `gradlew clean shadow`
- **Do not commit unless you are upgrading to a newer version!**

## Publishing

- Update the changelog
- Increase the version number in the build file. We use [semantic versioning](http://semver.org)!
- Create a git tag.
- Build the fat jar: `gradlew clean shadow`. The jar will be under build/libs.
- You may convert USAGE.md to a PDF with e.g. pandoc

