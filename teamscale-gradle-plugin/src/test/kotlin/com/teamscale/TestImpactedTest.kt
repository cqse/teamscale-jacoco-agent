package com.teamscale

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Unit tests for TestImpacted. */
class TestImpactedTest {

    @Test
    fun normalize() {
        assertThat(TestImpacted.normalize("**/com/teamscale/test/ui/**")).isEqualTo("**com.teamscale.test.ui**")
        assertThat(TestImpacted.normalize("**/TeamscaleUITest.class")).isEqualTo("**TeamscaleUITest")
    }

}