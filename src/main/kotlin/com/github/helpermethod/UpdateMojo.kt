package com.github.helpermethod

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.util.FS
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser.xmlParser

@Mojo(name = "update")
class UpdateMojo: AbstractMojo() {
    @Parameter(defaultValue = "\${project}", required = true)
    lateinit var mavenProject: MavenProject
    @Parameter(defaultValue = "\${localRepository}", required = true)
    lateinit var localRepository: ArtifactRepository
    @Parameter(defaultValue = "\${settings}", required = true)
    lateinit var settings: Settings
    @Parameter(property = "connectionUrl", defaultValue = "\${project.scm.connection}", required = true)
    lateinit var connectionUrl: String
    @Parameter(property = "developerConnectionUrl", defaultValue = "\${project.scm.developerConnection}", required = true)
    lateinit var developerConnectionUrl: String
    @Parameter(property = "connectionType", defaultValue = "connection", required = true)
    lateinit var connectionType: String
    @Component
    lateinit var artifactFactory: ArtifactFactory
    @Component
    lateinit var artifactMetadataSource: ArtifactMetadataSource

    private val connection: String
        get() = if (connectionType == "developerConnection") developerConnectionUrl else connectionUrl

    override fun execute() {
        withGit { git ->
            listOf(::parentUpdate, ::dependencyManagementUpdates, ::dependencyUpdates)
                .flatMap { it() }
                .map { it to "dependency-update/${it.groupId}-${it.artifactId}-${it.latestVersion}" }
                .filter { (_, branchName) -> git.hasRemoteBranch(branchName)}
                .forEach { (update, branchName) ->
                    git.checkout(branchName)
                    withPom(update.modification)
                    git.add("pom.xml")
                    git.commit("dependency-update-bot", "Bump $update.artifactId from $update.currentVersion to $update.latestVersion" )
                    git.push(branchName)
                }
        }
    }


    private fun withGit(f: (GitProvider) -> Unit) {
        val git = JGitProvider(Git(
            RepositoryBuilder()
                .readEnvironment()
                .findGitDir(mavenProject.basedir)
                .setMustExist(true)
                .build()
        ), connection, settings)

        git.use(f)
    }

    private fun withPom(f: (Document) -> Unit) {
        val pom = Jsoup.parse(mavenProject.file.readText(), "", xmlParser()).apply {
            outputSettings().prettyPrint(false)
        }

        f(pom)

        mavenProject.file.writeText(pom.html())
    }

    private fun parentUpdate() =
        listOfNotNull(mavenProject.parentArtifact)
            .mapNotNull { artifact ->
                update(artifact) { version, pom ->
                    pom.selectFirst("project > parent > version").text(version)
                }
            }

    private fun dependencyManagementUpdates() =
        (mavenProject.originalModel.dependencyManagement?.dependencies ?: listOf())
            .filter(Dependency::isConcrete)
            .map(artifactFactory::createDependencyArtifact)
            .mapNotNull { artifact ->
                update(artifact) { version, pom ->
                    pom.selectFirst("project > dependencyManagement > dependencies > dependency:has(> groupId:containsOwn(${artifact.groupId})):has(> artifactId:containsOwn(${artifact.groupId})):has(> version:containsOwn(${artifact.version})) > version").text(version)
                }
            }

    private fun dependencyUpdates() =
        mavenProject.originalModel.dependencies
            .filter(Dependency::isConcrete)
            .map(artifactFactory::createDependencyArtifact)
            .mapNotNull { artifact ->
                update(artifact) { version, pom ->
                    pom.selectFirst("project > dependencies > dependency:has(> groupId:containsOwn(${artifact.groupId})):has(> artifactId:containsOwn(${artifact.groupId})):has(> version:containsOwn(${artifact.version})) > version").text(version)
                }
            }

    private fun update(artifact: Artifact, f: (String, Document) -> Unit): Update? {
        val latestVersion = retrieveLatestVersion(artifact)

        if (artifact.version == latestVersion) return null

        return Update(groupId = artifact.groupId, artifactId = artifact.artifactId, version = artifact.version, latestVersion = latestVersion, modification = { pom: Document -> f(latestVersion, pom) })
    }

    private fun retrieveLatestVersion(artifact: Artifact) =
        artifactMetadataSource
            .retrieveAvailableVersions(artifact, localRepository, mavenProject.remoteArtifactRepositories)
            // TODO the filter should be made configurable with `SNAPSHOT` as default
            .filter { it.qualifier != "SNAPSHOT" }
            .max()
            .toString()

    data class Update(val groupId: String, val artifactId: String, val version: String, val latestVersion: String, val modification: (Document) -> Unit)
}