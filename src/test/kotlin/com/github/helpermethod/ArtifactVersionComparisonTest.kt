package com.github.helpermethod

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArtifactVersionComparisonTest {
    @Test
    internal fun sameVersionIsEqual() {
        val version = DefaultArtifactVersion("1.0.0")

        assertThat(version).isEqualTo(version)
    }

    @Test
    internal fun sameVersionIsEqualInKotlin() {
        val version = DefaultArtifactVersion("1.0.0")

        assertThat(version == version).isTrue()
    }

    @Test
    internal fun compareVersions() {
        val version1 = DefaultArtifactVersion("1.0.0")
        val version2 = DefaultArtifactVersion("1.2.0")

        assertThat(version1).isLessThan(version2)
        assertThat(version2).isGreaterThan(version1)
    }

    @Test
    internal fun compareVersionsKotlin() {
        val version1 = DefaultArtifactVersion("1.0.0")
        val version2 = DefaultArtifactVersion("1.2.0")

        assertThat(version1 < version2).isTrue()
        assertThat(version2 > version1).isTrue()
    }

    @Test
    internal fun canFindMax() {
        val versions = listOf(
            DefaultArtifactVersion("1.0.0"),
            DefaultArtifactVersion("1.3.0"),
            DefaultArtifactVersion("1.2.0")
        )

        assertThat(versions.max()).isEqualTo(DefaultArtifactVersion("1.3.0"))
    }
}