package com.github.helpermethod

import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser.xmlParser

@Mojo(name = "update")
class UpdateMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", required = true)
    lateinit var mavenProject: MavenProject
    @Parameter(defaultValue = "\${localRepository}", required = true)
    lateinit var localRepository: ArtifactRepository
    @Parameter(defaultValue = "\${settings}", required = true)
    lateinit var settings: Settings
    @Parameter(property = "connectionUrl", defaultValue = "\${project.scm.connection}", required = true)
    lateinit var connectionUrl: String
    @Parameter(
        property = "developerConnectionUrl",
        defaultValue = "\${project.scm.developerConnection}",
        required = true
    )
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
            UpdateResolver(
                mavenProject = mavenProject,
                artifactMetadataSource = artifactMetadataSource,
                localRepository = localRepository,
                createDependency = artifactFactory::createDependencyArtifact
            )
            .updates
            .map { it to "dependency-update/${it.groupId}-${it.artifactId}-${it.latestVersion}" }
            .filter { (_, branchName) -> git.hasRemoteBranch(branchName) }
            .forEach { (update, branchName) ->
                git.checkout(branchName)
                withPom(update.modification)
                git.add("pom.xml")
                git.commit(
                    "dependency-update-bot",
                    "Bump $update.artifactId from $update.currentVersion to $update.latestVersion"
                )
                git.push(branchName)
            }
        }
    }

    private fun withGit(f: (GitProvider) -> Unit) {
        val git = JGitProvider(
            Git(
                RepositoryBuilder()
                    .readEnvironment()
                    .findGitDir(mavenProject.basedir)
                    .setMustExist(true)
                    .build()
            ), connection, settings
        )

        git.use(f)
    }

    private fun withPom(f: (Document) -> Unit) {
        val pom = Jsoup.parse(mavenProject.file.readText(), "", xmlParser()).apply {
            outputSettings().prettyPrint(false)
        }

        f(pom)

        mavenProject.file.writeText(pom.html())
    }
}