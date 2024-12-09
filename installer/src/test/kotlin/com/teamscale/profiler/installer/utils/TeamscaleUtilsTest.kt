package com.teamscale.profiler.installer.utils

import com.teamscale.profiler.installer.FatalInstallerError
import com.teamscale.profiler.installer.TeamscaleCredentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class TeamscaleUtilsTest {
	@Test
	fun unresolvableUrl() {
		Assertions.assertThatThrownBy { checkTeamscaleConnection("http://totally.invalid:9999") }
			.hasMessageContaining("The host http://totally.invalid:9999/ could not be resolved")
	}

	@Test
	fun unreachableUrl() {
		Assertions.assertThatThrownBy { checkTeamscaleConnection("http://localhost:9999") }
			.hasMessageContaining("The host http://localhost:9999/ refused a connection")
	}

	@Test
	fun incorrectCredentials() {
		mockTeamscale?.setStatusCode(401)
		Assertions.assertThatThrownBy { checkTeamscaleConnection("http://localhost:$TEAMSCALE_PORT") }
			.hasMessageContaining("You provided incorrect credentials")
	}

	@Throws(FatalInstallerError::class)
	private fun checkTeamscaleConnection(url: String) {
		TeamscaleUtils.checkTeamscaleConnection(TeamscaleCredentials(url.toHttpUrl(), "user", "key"))
	}

	companion object {
		private var mockTeamscale: MockTeamscale? = null
		private const val TEAMSCALE_PORT = 8758

		@JvmStatic
		@BeforeAll
		fun startMockTeamscale() {
			mockTeamscale = MockTeamscale(TEAMSCALE_PORT)
		}

		@JvmStatic
		@AfterAll
		fun stopMockTeamscale() {
			mockTeamscale?.shutdown()
		}
	}
}