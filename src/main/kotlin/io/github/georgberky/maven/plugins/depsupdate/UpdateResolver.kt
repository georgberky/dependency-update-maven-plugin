package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

class UpdateResolver(
    private val mavenProject: MavenProject,
    private val artifactMetadataSource: ArtifactMetadataSource,
    private val localRepository: ArtifactRepository,
    private val artifactFactory: ArtifactFactory,
    private val mavenSession: MavenSession
) {
    val reactorDependencies
        get() = mavenSession.projects

    val updates
        get() =
            parentUpdates + dependencyManagementUpdates + dependencyUpdates

    val parentUpdates
        get() = listOfNotNull(mavenProject.parentArtifact)
                .map { artifact -> ParentVersionUpdate(
                        artifact.groupId,
                        artifact.artifactId,
                        artifact.version,
                        retrieveLatestVersion(artifact),
                        pomXml()) }

    val dependencyManagementUpdates
        get() = (mavenProject.originalModel.dependencyManagement?.dependencies ?: listOf())
            .filter(Dependency::isConcrete)
            .filterNot { isReactorArtifact(it) }
            .mapNotNull(artifactFactory::createDependencyArtifact)
            .map { artifact -> DependencyManagementVersionUpdate(
                    artifact.groupId,
                    artifact.artifactId,
                    artifact.version,
                    retrieveLatestVersion(artifact),
                    pomXml())}

    val dependencyUpdates
        get() = mavenProject.originalModel.dependencies
            .filter(Dependency::isConcrete)
            .filterNot { isReactorArtifact(it) }
            .onEach { println("dependencyUpdates: $it") }
            .mapNotNull(artifactFactory::createDependencyArtifact)
            .map { a -> DependencyVersionUpdate(
                    a.groupId,
                    a.artifactId,
                    a.version,
                    retrieveLatestVersion(a),
                    pomXml())}

    /**
     * Returns the latest version or (if not resolvable) the String {@code "null"}.
     *
     * @return the latest version as String or the String {@code "null"} if no latest version was found.
     */
    private fun retrieveLatestVersion(artifact: Artifact) =
        artifactMetadataSource
            .retrieveAvailableVersions(artifact, localRepository, mavenProject.remoteArtifactRepositories)
            .onEach { println("retrieveLatestVersion: $it") }
            .filter { it.qualifier != "SNAPSHOT" }
            .maxOrNull()
            .toString()

    private fun isReactorArtifact(dependency: Dependency): Boolean =
        reactorDependencies
            .any { module ->
                dependency.groupId == module.groupId
                        && dependency.artifactId == module.artifactId
                        && dependency.version == module.version
                        && dependency.type == module.packaging
            }

    private fun pomXml() : Document {
        return Jsoup.parse(mavenProject.file.readText(), "", Parser.xmlParser())
                .apply { outputSettings().prettyPrint(false) }
    }
}
