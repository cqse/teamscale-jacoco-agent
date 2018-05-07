The contents of this directory can be used to install the Teamscale JaCoCo client as a windows
service. This ensures that it is automatically restarted on system reboot.

To install the service first configure the client and ensure it can
be started normally. Then follow the next steps to install:

1. Edit the file teamscale-service.xml and follow the TODO comments
2. Optionally uncomment and fill the section `<serviceaccount>` to run the
   service as a different user. Alternatively you can set this information
   after installing using the service console, which avoids storing the
   password in clear text.
   In both cases the user needs to have the "Logon as a service" right.
   This right can be granted in the Local Security Policy management console
   by navigating to "Local Policies > User Rights Assignment" and adding the
   user to "Logon as a service".

3. Install the service by running from the command line:

        teamscale-jacoco-client-service.exe install

4. Check that the service is running. Directly after installation, you
   might have to start the services manually or alternatively reboot the
   server.

If you later have to uninstall the service run the following commands:

    teamscale-jacoco-client-service.exe uninstall


