package com.github.helpermethod

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject
import org.jsoup.nodes.Document

class UpdateResolver(
    private val mavenProject: MavenProject,
    private val artifactMetadataSource: ArtifactMetadataSource,
    private val localRepository: ArtifactRepository,
    private val createDependency: (dependency: Dependency) -> Artifact
) {
    val updates
        get() =
            parentUpdates + dependencyManagementUpdates + dependencyUpdates

    val parentUpdates
        get() =
            listOfNotNull(mavenProject.parentArtifact)
                .mapNotNull { artifact ->
                    update(artifact) { version, pom ->
                        pom.selectFirst("project > parent > version").text(version)
                    }
                }

    val dependencyManagementUpdates
        get() =
            (mavenProject.originalModel.dependencyManagement?.dependencies ?: listOf())
                .filter(Dependency::isConcrete)
                .map(createDependency)
                .mapNotNull { artifact ->
                    update(artifact) { version, pom ->
                        pom.selectFirst("project > dependencyManagement > dependencies > dependency:has(> groupId:containsOwn(${artifact.groupId})):has(> artifactId:containsOwn(${artifact.artifactId})):has(> version:containsOwn(${artifact.version})) > version")
                            .text(version)
                    }
                }

    val dependencyUpdates
        get() =
            mavenProject.originalModel.dependencies
                .filter(Dependency::isConcrete)
                .map(createDependency)
                .mapNotNull { artifact ->
                    update(artifact) { version, pom ->
                        pom.selectFirst("project > dependencies > dependency:has(> groupId:containsOwn(${artifact.groupId})):has(> artifactId:containsOwn(${artifact.artifactId})):has(> version:containsOwn(${artifact.version})) > version")
                            .text(version)
                    }
                }

    private fun update(artifact: Artifact, f: (String, Document) -> Unit): Update? {
        val latestVersion = retrieveLatestVersion(artifact)

        if (artifact.version == latestVersion) return null

        return Update(
            groupId = artifact.groupId,
            artifactId = artifact.artifactId,
            version = artifact.version,
            latestVersion = latestVersion,
            modification = { pom: Document -> f(latestVersion, pom) })
    }

    private fun retrieveLatestVersion(artifact: Artifact) =
        artifactMetadataSource
            .retrieveAvailableVersions(artifact, localRepository, mavenProject.remoteArtifactRepositories)
            .filterNot { it.qualifier != "SNAPSHOT" }
            .max()
            .toString()
}