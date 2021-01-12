package com.github.helpermethod

import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArtifactVersionComparisonTest {
    @Test
    internal fun sameVersionIsEqual() {
        val version = DefaultArtifactVersion("1.0.0")

        assertThat(version).isEqualTo(version)
    }
}