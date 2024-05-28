# Teamscale Maven Plugin Dev Setup

# Import into IntelliJ 
The JaCoCo agent repo uses gradle, but since we are developing a maven plugin, it also uses maven as nested build system. To get code completion etc. in IntelliJ, open the maven tool window and click on sync (🔄) there. This will un-sync the gradle projects but instead sync the `teamscale-maven-plugin` project

# Manual testing
When doing manual tests, you can attach the IntelliJ debugger to the JVM in which the tests are run. Surefire (maven unit tests) creates its own JVM and you have to tell it to stop before the execution of the tests to attach a debugger. This can be done with `-Dmaven.surefire.debug`. 
You also need to have a build of the plugin in your local maven cache (usually `$HOME/.m2`) by calling the `./gradlew publishToMavenLocal` in the root of this repository (not root the root of the maven plugin).

So if you want to manually test the changes to the plugin you need to:
* Call `./gradlew publishToMavenLocal` to publish your plugin changes to your local maven cache
* Create a maven project with some tests and installed Teamscale plugin (see [Docs](https://docs.teamscale.com/howto/providing-testwise-coverage/#tia-with-maven-and-junit-5)). The version should be the one stated in the root level `build.gradle.kts` file with a `-SNAPSHOT` suffix, e.g. `33.2.0-SNAPSHOT`.
* In your maven project, call `mvn -Dmaven.surefire.debug verify`. The tests won't run until you do the next step.
* In IntelliJ, [create a Remote JVM debug run config](https://www.jetbrains.com/help/idea/attach-to-process.html#attach-to-remote) with default values. Set breakpoints in the code as needed and run the remote debug run config. The tests will resume now and your breakpoints in the plugin will be hit.