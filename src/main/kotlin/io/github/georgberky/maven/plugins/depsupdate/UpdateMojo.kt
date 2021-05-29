package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings

/**
 * The update mojo goes through all dependencies in the project and
 * create an own branch per dependency with its newest version.
*/
@Mojo(name = "update")
class UpdateMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${session}", readonly = true, required = true)
    lateinit var mavenSession: MavenSession

    @Parameter(defaultValue = "\${project}", required = true)
    lateinit var mavenProject: MavenProject

    @Parameter(defaultValue = "\${localRepository}", required = true)
    lateinit var localRepository: ArtifactRepository

    @Parameter(defaultValue = "\${settings}", required = true)
    lateinit var settings: Settings

    /**
     * Currently, it has to be set, but it is only necessary for the Git provider JGIT.
     */
    @Parameter(property = "connectionUrl", defaultValue = "\${project.scm.connection}", required = false)
    var connectionUrl: String = ""

    /**
     * Currently, it has to be set, but it is only necessary for the Git provider JGIT.
     */
    @Parameter(property = "developerConnectionUrl", defaultValue = "\${project.scm.developerConnection}", required = false)
    var developerConnectionUrl: String = ""

    /**
     * Currently, it has to be set, but it is only necessary for the Git provider JGIT.
     */
    @Parameter(property = "connectionType", defaultValue = "connection", required = true)
    lateinit var connectionType: String

    /**
     * Set which Git provider should use, the installed git binary on your systen (NATIVE) or JGit (JGIT).
     */
    @Parameter(property = "dependencyUpdate.git.provider", defaultValue = "NATIVE", required = false)
    lateinit var gitProvider: GitProviderChoice

    @Component
    lateinit var artifactFactory: ArtifactFactory

    @Component
    lateinit var artifactMetadataSource: ArtifactMetadataSource

    private val connection: String
        get() = if (connectionType == "developerConnection") developerConnectionUrl else connectionUrl

    override fun execute() {
        withGit { git ->
            val updateCandidates = UpdateResolver(
                mavenProject = mavenProject,
                artifactMetadataSource = artifactMetadataSource,
                localRepository = localRepository,
                artifactFactory = artifactFactory,
                mavenSession = mavenSession
            )
                .updates
                .onEach { log.debug("execute in mojo: latestVersion: '${it.latestVersion}' / version:'${it.version}'") }
                .filterNot(VersionUpdate::isAlreadyLatestVersion)
                .onEach { log.debug("execute in mojo after latest version check: ${it.latestVersion}") }
                .map { it to UpdateBranchName(it.groupId, it.artifactId, it.latestVersion) }
                .onEach { log.debug("execute in mojo canSkip (after map): ${it.second} ${it.first}") }

            val updateCandidatesWithExistingUpdateBranch = updateCandidates
                .filter { (_, branchName) -> git.hasRemoteBranchWithPrefix(branchName.prefix()) }

            updateCandidatesWithExistingUpdateBranch
                .forEach { (_, branchName) ->
                    log.warn(
                        "Dependency '${branchName.prefix()}' already has a branch for a previous version update. " +
                            "Please merge it first"
                    )
                }

            val updatesWithoutBranchForVersion =
                updateCandidates.filterNot { (_, branchName) -> git.hasRemoteBranchWithPrefix(branchName.prefix()) }

            updatesWithoutBranchForVersion
                .onEach { log.debug("execute in mojo after filter branches: ${it.second} ${it.first}") }
                .forEach { (update, branchName) ->
                    git.checkoutNewBranch(branchName.toString())
                    val pom = update.updatedPom()
                    mavenProject.file.writeText(pom.html())
                    git.add("pom.xml")
                    git.commit(
                        "dependency-update-bot",
                        "",
                        "Bump ${update.artifactId} from ${update.version} to ${update.latestVersion}"
                    )
                    git.push(branchName.toString())
                    git.checkoutInitialBranch()
                }
        }
    }

    private fun withGit(f: (GitProvider) -> Unit) {
        val git = gitProvider.createProvider(mavenProject.basedir.toPath(), settings, connection)
        git.use(f)
    }

    data class UpdateBranchName(val groupId: String, val artifactId: String, val version: String) {
        fun prefix(): String {
            return "dependency-update/$groupId-$artifactId"
        }

        override fun toString(): String {
            return "${prefix()}-$version"
        }
    }
}