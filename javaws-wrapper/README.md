# Wrapper around javaws to allow profiling Web Start applications

## Rationale

The normal JavaWS startup mechanism does not allow us to inject the `-javaagent` argument into the final JVM that is required to install the profiler's Java agent that records test coverage.
The `java-vm-args` property of JNLP files unfortunately [does not allow setting the `-javaagent` VM argument](https://docs.oracle.com/javase/7/docs/technotes/guides/javaws/developersguide/syntax.html).

Additionally, the default security policy for JavaWS does not allow the agent to read system properties, write the coverage information to disk or to contact Teamscale via HTTP(S) (and possibly more).

## Alternatives

One workaround is to set `-javaagent` via `JAVA_TOOL_OPTIONS`.
The `javaws` process must see this environment variable and will then apply these options to the spawned VM.
However, this usually means the environment variable must be set system-wide, i.e. all Java applications will be profiled.

## Installation

Please review the Security section below before installing!

Put all files in any directory, as long as it is readable by all users that want to start
Web Start applications.

Under Windows, run `bin\javaws install` as an administrator. This will register the wrapper as the handler for JNLP files
(for the current user) and fix the security policy file (for the JVM installation under which it is run).

Under Linux, associate JNLP files with the wrapper (`bin/javaws`). If necessary, replace the JVM's `javaws.policy`
with the contents of the `agent.policy`.

## Uninstallation

For Windows, run `bin\javaws uninstall`. This will restore the old file type mapping (for the current user)
and the javaws security policy file (for the JVM installation under which it is run).

Under Linux, revert whatever you did to associate the wrapper with JNLP files and undo any changes to the `javaws.policy`.

## Configuration

The wrapper expects a file called `javaws.properties` in the same directory as the wrapper is located.
An example is provided. If you used the Windows installer (see above), this will be partly preconfigured for you.
Please fill out all properties in that file before first use of the wrapper.

The `agentArguments` must be filled according to the guidelines from the Teamscale JaCoCo agent's userguide
(see the PDF in the `docs` folder). In most cases, it is enough to adjust the `includes` parameter to
your code base.

As per the Java properties file spec, the file must use the ISO 8859-1 encoding!
Please remember that backslashes have to be escaped in the properties file! E.g.:

    javaws=C:\\Program Files (x86)\\Java\\jre1.8.0_171\\bin\\javaws.exe

Alternatively, just use forward slashes. They work under Windows as well!

## Security

In order for the wrapper to be able to profile classes and send the data to disk or Teamscale, it needs
more permissions than a Java Web Start application normally has. These are granted by the `agent.policy`
policy file. Reducing these permissions may result in the wrapper failing with security exceptions or
failing silently without the application launching - i.e. it won't work.

In order for the wrapper to work under Windows under all JVMs, the Windows installer replaces the JVM
installation's `javaws.policy` file, i.e. security settings for all Java Web Start processes are
changed! Please make sure that this is OK before installing this tool.

