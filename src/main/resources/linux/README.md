The contents of this directory can be used to install the JaCoCo client as a
Linux _systemd_ service. This ensures that the client is automatically restarted on
system reboot.

To see if your system supports systemd, check for the existance of the directory
`/lib/systemd`

To install the service:

- Modify the teamscale-jacoco-client.service file according to the TODO comments inside.
- Copy the file to `/etc/systemd/system`
- Enable the service to start at boot time by running

        sudo systemctl enable teamscale-jacoco-client.service

To uninstall the service

1. Run

        sudo systemctl stop teamscale-jacoco-client.service
        sudo systemctl disable teamscale-jacoco-client.service

2. remove the file you copied to `/etc/systemd/system/teamscale-jacoco-client.service`

To start the service manually use

    sudo systemctl start teamscale-jacoco-client.service

_This procedure was tested on Ubuntu 17.04_

