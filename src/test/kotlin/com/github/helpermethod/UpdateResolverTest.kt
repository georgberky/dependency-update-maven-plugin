package com.github.helpermethod

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.plugin.testing.ArtifactStubFactory
import org.apache.maven.project.MavenProject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
internal class UpdateResolverTest {
    @MockK
    lateinit var metadataSourceMock : ArtifactMetadataSource

    @MockK
    lateinit var localRepositoryMock : ArtifactRepository

    @MockK
    lateinit var artifactFactoryMock : ArtifactFactory

    @Test
    fun parentUpdate() {
        val project = projectWithParent()
        every { metadataSourceMock.retrieveAvailableVersions(project.parentArtifact, any(), any()) } returns listOf( DefaultArtifactVersion("1.0.0") )

        val updateResolverUnderTest = UpdateResolver(project, metadataSourceMock, localRepositoryMock, artifactFactoryMock)

        assertThat(updateResolverUnderTest.parentUpdates)
                .hasSize(1)
                .extracting("groupId", "artifactId", "version", "latestVersion")
                .containsExactly(tuple(
                        project.parentArtifact.groupId,
                        project.parentArtifact.artifactId,
                        project.parentArtifact.version,
                        "1.0.0"))
    }

    private fun projectWithParent(): MavenProject {
        val project = MavenProject()
        project.parentArtifact = artifact("com.github.helpermethod", "dependency-update-parent", "0.6.0")
        project.file = File("src/test/resources/poms/parentPom.xml")
        return project
    }

    private fun artifact(groupId: String, artifactId: String, version: String) =
            ArtifactStubFactory().createArtifact(groupId, artifactId, version)
}