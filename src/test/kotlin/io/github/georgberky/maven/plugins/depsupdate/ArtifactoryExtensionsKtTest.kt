package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.DefaultArtifactFactory
import org.apache.maven.artifact.handler.ArtifactHandler
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.artifact.versioning.VersionRange
import org.assertj.core.api.Assertions.assertThat
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager
import org.apache.maven.model.Dependency
import org.assertj.core.api.Assertions.`in`

internal class ArtifactoryExtensionsKtTest {

    @CartesianTest
    fun `returns NULL if scope is test or provided`(
        @CartesianTest.Values(strings = [ "somegroup" ]) groupId: String,
        @CartesianTest.Values(strings = [ "", "someArtifactId" ]) artifactId: String,
        @CartesianTest.Values(strings = [ "", "1.0.0", "1", "1.FINAL", "[1.0,2.0)" ]) versionSpec: String,
        @CartesianTest.Values(strings = [ "", "jar", "pom", "bundle" ]) type: String,
        @CartesianTest.Values(strings = [ "", "classes" ]) classifier: String,
        @CartesianTest.Values(strings = [ "", "provided", "test", "compile", "runtime", "import", "system" ]) scope: String,
        @CartesianTest.Values(strings = [ "true", "false" ]) inheritedScope: String
    ) {
        // given
        val artifactHandlerManager = object: ArtifactHandlerManager {
            override fun getArtifactHandler(type: String?): ArtifactHandler {
                return DefaultArtifactHandler()
            }

            override fun addHandlers(handlers: MutableMap<String, ArtifactHandler>?) {
                TODO("Not yet implemented")
            }
        }
        val defaultArtifactFactory = DefaultArtifactFactory()
        val declaredField = defaultArtifactFactory.javaClass.getDeclaredField("artifactHandlerManager")
        declaredField.isAccessible = true
        declaredField.set(defaultArtifactFactory, artifactHandlerManager)
        val dependency = Dependency()
        dependency.groupId = groupId
        dependency.artifactId = artifactId
        dependency.version = versionSpec
        dependency.classifier = classifier
        dependency.scope = scope
        dependency.optional = inheritedScope

        // when
        val artifact: Artifact? = defaultArtifactFactory.createDependencyArtifact(dependency)

        // expect
        if (scope == "provided" || scope == "test")
            assertThat(artifact).isNull()
        else
            assertThat(artifact).isNotNull
    }
}
