package eu.cqse.teamscale.client;

import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ServerAndCommitInfos extends Server implements Serializable {
	public String partition;
	public String message;
	public CommitDescriptor commitDescriptor;

	public void setServer(Server server) {
		this.url = server.url;
		this.project = server.project;
		this.userName = server.userName;
		this.userAccessToken = server.userAccessToken;
	}
}
