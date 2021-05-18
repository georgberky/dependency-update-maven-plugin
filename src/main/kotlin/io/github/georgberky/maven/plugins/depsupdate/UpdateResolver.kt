package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

class UpdateResolver(
        private val mavenProject: MavenProject,
        private val artifactMetadataSource: ArtifactMetadataSource,
        private val localRepository: ArtifactRepository,
        private val artifactFactory: ArtifactFactory
) {
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
            .map(artifactFactory::createDependencyArtifact)
            .map { artifact -> DependencyManagementVersionUpdate(
                    artifact.groupId,
                    artifact.artifactId,
                    artifact.version,
                    retrieveLatestVersion(artifact),
                    pomXml())}

    val dependencyUpdates
        get() = mavenProject.originalModel.dependencies
            .filter(Dependency::isConcrete)
            .onEach { println("dependencyUpdates: $it") }
            .map(artifactFactory::createDependencyArtifact)
            .map { a -> DependencyVersionUpdate(
                    a.groupId,
                    a.artifactId,
                    a.version,
                    retrieveLatestVersion(a),
                    pomXml())}

    private fun retrieveLatestVersion(artifact: Artifact) =
        artifactMetadataSource
            .retrieveAvailableVersions(artifact, localRepository, mavenProject.remoteArtifactRepositories)
            .onEach { println("retrieveLatestVersion: $it") }
            .filter { it.qualifier != "SNAPSHOT" }
            .max()
            .toString()

    private fun pomXml() : Document {
        return Jsoup.parse(mavenProject.file.readText(), "", Parser.xmlParser())
                .apply { outputSettings().prettyPrint(false) }
    }
}