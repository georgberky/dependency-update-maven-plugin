package io.github.georgberky.maven.plugins.depsupdate

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Dependency
import org.apache.maven.model.DependencyManagement
import org.apache.maven.model.Model
import org.apache.maven.plugin.testing.ArtifactStubFactory
import org.apache.maven.project.MavenProject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
internal class UpdateResolverTest {
    private val junitArtifact = artifact("org.junit.jupiter", "junit-jupiter", "5.5.2")
    private val junitDependency = dependency("org.junit.jupiter", "junit-jupiter", "5.5.2")
    private val assertJDependency = dependency("org.assertj", "assertj-core", "3.15.0")
    private val assertJArtifact = artifact("org.assertj", "assertj-core", "3.15.0")
    private val reactorModuleA = dependency("com.text", "moduleA", "1.0.0-SNAPSHOT")
    private val reactorModuleAArtifact = artifact("com.text", "moduleA", "1.0.0-SNAPSHOT")
    private val reactorModuleB = dependency("com.text", "moduleB", "1.0.0-SNAPSHOT")
    private val reactorModuleBArtifact = artifact("com.text", "moduleB", "1.0.0-SNAPSHOT")

    @MockK
    lateinit var metadataSourceMock : ArtifactMetadataSource

    @MockK
    lateinit var localRepositoryMock : ArtifactRepository

    @MockK
    lateinit var artifactFactoryMock : ArtifactFactory

    @MockK
    lateinit var mavenSession: MavenSession

    @BeforeEach
    fun setUpMocks() {
        every { mavenSession.projects } returns listOf()
    }

    @Test
    fun parentUpdate() {
        val project = projectWithParent()
        every { metadataSourceMock.retrieveAvailableVersions(project.parentArtifact, any(), any()) } returns listOf( DefaultArtifactVersion("1.0.0") )

        val updateResolverUnderTest = UpdateResolver(
            project,
            metadataSourceMock,
            localRepositoryMock,
            artifactFactoryMock,
            mavenSession
        )

        assertThat(updateResolverUnderTest.parentUpdates)
                .hasSize(1)
                .extracting("groupId", "artifactId", "version", "latestVersion")
                .containsExactly(tuple(
                        project.parentArtifact.groupId,
                        project.parentArtifact.artifactId,
                        project.parentArtifact.version,
                        "1.0.0"))
    }

    @Test
    fun dependencyUpdates() {
        val project = projectWithDependencies()
        every { metadataSourceMock.retrieveAvailableVersions(any(), any(), any()) } returns listOf( DefaultArtifactVersion("6.0.0") )
        every { artifactFactoryMock.createDependencyArtifact(junitDependency) } returns junitArtifact
        every { artifactFactoryMock.createDependencyArtifact(assertJDependency) } returns assertJArtifact

        val updateResolverUnderTest = UpdateResolver(
            project,
            metadataSourceMock,
            localRepositoryMock,
            artifactFactoryMock,
            mavenSession
        )

        assertThat(updateResolverUnderTest.dependencyUpdates)
                .hasSize(2)
                .extracting("groupId", "artifactId", "version", "latestVersion")
                .containsExactly(tuple("org.junit.jupiter", "junit-jupiter", "5.5.2","6.0.0"),
                                 tuple("org.assertj", "assertj-core", "3.15.0", "6.0.0"))
    }

    @Test
    fun dependencyManagementUpdates() {
        val project = projectWithDependenciesManagement()
        every { metadataSourceMock.retrieveAvailableVersions(any(), any(), any()) } returns listOf( DefaultArtifactVersion("6.0.0") )
        every { artifactFactoryMock.createDependencyArtifact(junitDependency) } returns junitArtifact
        every { artifactFactoryMock.createDependencyArtifact(assertJDependency) } returns assertJArtifact

        val updateResolverUnderTest = UpdateResolver(
            project,
            metadataSourceMock,
            localRepositoryMock,
            artifactFactoryMock,
            mavenSession
        )

        assertThat(updateResolverUnderTest.dependencyManagementUpdates)
                .hasSize(2)
                .extracting("groupId", "artifactId", "version", "latestVersion")
                .containsExactly(tuple("org.junit.jupiter", "junit-jupiter", "5.5.2","6.0.0"),
                        tuple("org.assertj", "assertj-core", "3.15.0", "6.0.0"))
    }

    @Test
    fun reactorProjectSkipsReactorModules() {
        val project = projectWithReactorDependencyManagement()

        val reactorModelA = Model()
        reactorModelA.groupId = reactorModuleA.groupId
        reactorModelA.artifactId = reactorModuleA.artifactId
        reactorModelA.version = reactorModuleA.version
        reactorModelA.packaging = "jar"
        val reactorModuleAProject = MavenProject(reactorModelA)

        val reactorModelB = Model()
        reactorModelB.groupId = reactorModuleB.groupId
        reactorModelB.artifactId = reactorModuleB.artifactId
        reactorModelB.version = reactorModuleB.version
        reactorModelB.packaging = "jar"
        val reactorModuleBProject = MavenProject(reactorModelB)


        every { mavenSession.projects } returns listOf( reactorModuleAProject, reactorModuleBProject )
        every { artifactFactoryMock.createDependencyArtifact(reactorModuleA) } returns reactorModuleAArtifact
        every { artifactFactoryMock.createDependencyArtifact(reactorModuleB) } returns reactorModuleBArtifact
        every { metadataSourceMock.retrieveAvailableVersions(any(), any(), any()) } returns listOf( DefaultArtifactVersion("6.0.0") )

        val updateResolverUnderTest = UpdateResolver(
            project,
            metadataSourceMock,
            localRepositoryMock,
            artifactFactoryMock,
            mavenSession
        )

        assertThat(updateResolverUnderTest.dependencyManagementUpdates)
            .hasSize(0)
    }

    @Test
    fun nullDependencyUpdates() {
        // given
        val project = projectWithDependencies()
        val updateResolverUnderTest = UpdateResolver(
            project,
            metadataSourceMock,
            localRepositoryMock,
            artifactFactoryMock,
            mavenSession
        )
        every { artifactFactoryMock.createDependencyArtifact(junitDependency) } returns null
        every { artifactFactoryMock.createDependencyArtifact(assertJDependency) } returns assertJArtifact

        // when
        val dependencyManagementUpdates = updateResolverUnderTest.dependencyManagementUpdates

        // then
        assertThat(dependencyManagementUpdates)
            .hasSize(0)
    }

    private fun projectWithParent(): MavenProject {
        val project = MavenProject()
        project.parentArtifact = artifact("io.github.georgberky.maven.plugins.depsupdate", "dependency-update-parent", "0.6.0")
        project.file = File("src/test/resources/poms/parentPom.xml")
        return project
    }

    private fun projectWithDependencies(): MavenProject {
        val project = MavenProject()
        val originalModel = Model()
        originalModel.dependencies = listOf(junitDependency,assertJDependency)
        project.originalModel = originalModel
        project.file = File("src/test/resources/poms/dependenciesPom.xml")
        return project
    }

    private fun projectWithDependenciesManagement(): MavenProject {
        val project = MavenProject()
        val originalModel = Model()
        val dependencyManagement = DependencyManagement()
        dependencyManagement.dependencies = listOf(junitDependency,assertJDependency)
        originalModel.dependencyManagement = dependencyManagement
        project.originalModel = originalModel
        project.file = File("src/test/resources/poms/dependencyManagementPom.xml")
        return project
    }

    private fun projectWithReactorDependencyManagement(): MavenProject {
        val project = MavenProject()
        val originalModel = Model()
        val dependencyManagement = DependencyManagement()
        dependencyManagement.dependencies = listOf(reactorModuleA, reactorModuleB)
        originalModel.dependencyManagement = dependencyManagement
        project.originalModel = originalModel
        project.file = File("src/test/resources/poms/dependencyManagementPom.xml")
        return project
    }

    private fun dependency(groupId: String, artifactId: String, version: String): Dependency {
        val dependency = Dependency()
        dependency.groupId = groupId
        dependency.artifactId = artifactId
        dependency.version = version
        return dependency
    }

    private fun artifact(groupId: String, artifactId: String, version: String) =
        ArtifactStubFactory().createArtifact(
            groupId,
            artifactId,
            VersionRange.createFromVersion(version),
            "compile",
            "jar",
            "",
            false
        )
}
