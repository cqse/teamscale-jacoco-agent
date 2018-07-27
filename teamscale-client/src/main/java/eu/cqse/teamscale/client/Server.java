package eu.cqse.teamscale.client;

import java.util.Objects;

public class Server {

	/** The url of the teamscale server.  */
	public String url;

	/** The project id for which artifacts should be uploaded.  */
	public String project;

	/** The user name of the Teamscale user.  */
	public String userName;

	/** The access token of the user.  */
	public String userAccessToken;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Server server = (Server) o;
		return Objects.equals(url, server.url) &&
				Objects.equals(project, server.project) &&
				Objects.equals(userName, server.userName) &&
				Objects.equals(userAccessToken, server.userAccessToken);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, project, userName, userAccessToken);
	}
}
