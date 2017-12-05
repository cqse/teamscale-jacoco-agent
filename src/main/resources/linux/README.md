The contents of this directory can be used to install the JaCoCo client as a
Linux service. This ensures that the client is automatically restarted on
system reboot. Currently, only a _systemd_ unit file is provided.

To see if your system supports systemd, check for the existance of the directory
`/lib/systemd`

To install the service:

- Modify the cqse-jacoco-client.service file according to the TODO comments inside.
- Copy the file to `/etc/systemd/system`

To uninstall the service simply remove the file you copied above.

