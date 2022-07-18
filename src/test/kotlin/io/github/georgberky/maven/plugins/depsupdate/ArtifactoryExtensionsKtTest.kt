package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.DefaultArtifactFactory
import org.apache.maven.artifact.versioning.VersionRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ArtifactoryExtensionsKtTest {

    @Test
    fun `returns NULL if scope is test or provided`() {
        // given
        val artifact: Artifact? = DefaultArtifactFactory().createDependencyArtifact(
            "someGroupId",
            "someArtifactId",
            VersionRange.createFromVersionSpec("1.0.0"),
            "jar",
            "",
            "test",
            "false"
        )

        // expect
        assertThat(artifact)
            .isNull()
    }
}
