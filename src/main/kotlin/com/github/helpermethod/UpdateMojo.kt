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
import org.codehaus.stax2.XMLInputFactory2
import org.codehaus.stax2.XMLInputFactory2.P_PRESERVE_LOCATION
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.*
import org.jdom2.Document
import org.jdom2.filter.Filters.element
import org.jdom2.input.StAXStreamBuilder
import org.jdom2.output.Format
import org.jdom2.output.LineSeparator.NONE
import org.jdom2.output.XMLOutputter
import org.jdom2.xpath.XPathFactory
import javax.xml.stream.util.StreamReaderDelegate

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
        val xmlInputFactory = XMLInputFactory2.newInstance().apply {
            setProperty(P_PRESERVE_LOCATION, true)
        }
        val namespaceIgnoringStreamReder = object: StreamReaderDelegate(xmlInputFactory.createXMLStreamReader(mavenProject.file.reader())) {
            override fun getAttributeNamespace(index: Int) = ""

            override fun getNamespaceURI(index: Int) = ""
        }
        val pom = StAXStreamBuilder().build(namespaceIgnoringStreamReder)

        f(pom)

        XMLOutputter(Format.getRawFormat().setLineSeparator(NONE)).output(pom, mavenProject.file.writer())
    }

    private fun parentUpdate() =
        listOfNotNull(mavenProject.parentArtifact)
            .mapNotNull { artifact ->
                update(artifact) { version, pom ->
                    XPathFactory.instance().compile("/project/parent/version", element()).evaluateFirst(pom)?.text = version
                }
            }

    private fun dependencyManagementUpdates() =
        (mavenProject.originalModel.dependencyManagement?.dependencies ?: listOf())
            .filter(Dependency::isConcrete)
            .map(artifactFactory::createDependencyArtifact)
            .mapNotNull { artifact ->
                update(artifact) { version, pom ->
                    XPathFactory.instance().compile("/project/dependencyManagement/dependencies/dependency[groupId = '${artifact.groupId}' and artifactId = '${artifact.artifactId}' and version = '${artifact.version}']/version", element()).evaluateFirst(pom)?.text = version
                }
            }

    private fun dependencyUpdates() =
        mavenProject.originalModel.dependencies
            .filter(Dependency::isConcrete)
            .map(artifactFactory::createDependencyArtifact)
            .mapNotNull { artifact ->
                update(artifact) { version, pom ->
                    XPathFactory.instance().compile("/project/dependencies/dependency[groupId = '${artifact.groupId}' and artifactId = '${artifact.artifactId}' and version = '${artifact.version}']/version", element()).evaluateFirst(pom)?.text = version
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
            .filter { it.qualifier != "SNAPSHOT" }
            .max()
            .toString()

    data class Update(val groupId: String, val artifactId: String, val version: String, val latestVersion: String, val modification: (Document) -> Unit)
}