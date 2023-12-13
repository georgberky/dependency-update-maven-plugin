package io.github.georgberky.maven.plugins.depsupdate

import com.github.sparsick.testcontainers.gitserver.GitServerContainer
import com.jcraft.jsch.Session
import com.soebes.itf.jupiter.extension.MavenGoal
import com.soebes.itf.jupiter.extension.MavenJupiterExtension
import com.soebes.itf.jupiter.extension.MavenOption
import com.soebes.itf.jupiter.extension.MavenProject
import com.soebes.itf.jupiter.extension.MavenTest
import com.soebes.itf.jupiter.maven.MavenExecutionResult
import com.soebes.itf.jupiter.maven.MavenProjectResult
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.util.stream.Collectors.toList
import com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat as mavenAssertThat

@MavenJupiterExtension
@MavenProject("jgitProviderIsSet_scmConfigIsSet")
@Testcontainers(disabledWithoutDocker = true)
internal class UpdateMojoJGitIT {

    @Container
    private val gitServer = GitServerContainer(DockerImageName.parse("rockstorm/git-server:2.40"))

    @TempDir
    lateinit var remoteRepo: File

    lateinit var repo: Git

    @BeforeEach
    internal fun setUp(result: MavenProjectResult) {
        val sshSessionFactory = object : JschConfigSessionFactory() {
            override fun configure(host: OpenSshConfig.Host?, session: Session?) {
                session?.setPassword("12345")
                session?.setConfig("StrictHostKeyChecking", "no")
            }
        }

        prepareRemoteRepo(sshSessionFactory, result)

        FileUtils.deleteDirectory(result.targetProjectDirectory)

        repo = Git.cloneRepository()
            .setURI(gitServer.getGitRepoURIAsSSH().toString())
            .setDirectory(result.targetProjectDirectory)
            .setBranch("main")
            .setTransportConfigCallback({ transport ->
                val sshTransport = transport as SshTransport
                sshTransport.sshSessionFactory = sshSessionFactory
            })
            .call()
    }

    private fun prepareRemoteRepo(sshSessionFactory: JschConfigSessionFactory, result: MavenProjectResult) {
        // TODO: replace scm connection in pom.xml
        val remoteRepoGit = Git.cloneRepository()
            .setURI(gitServer.getGitRepoURIAsSSH().toString())
            .setDirectory(remoteRepo)
            .setBranch("main")
            .setTransportConfigCallback { transport ->
                val sshTransport = transport as SshTransport
                sshTransport.sshSessionFactory = sshSessionFactory
            }
            .call()

        FileUtils.copyDirectory(result.targetProjectDirectory, remoteRepo)
        remoteRepoGit.add().addFilepattern(".").call()
        remoteRepoGit.commit()
            .setAuthor("Schorsch", "georg@email.com")
            .setMessage("Initial commit.")
            .call()

        remoteRepoGit.push()
            .setTransportConfigCallback { transport ->
                val sshTransport = transport as SshTransport
                sshTransport.sshSessionFactory = sshSessionFactory
            }.call()
    }

    @MavenTest
    @MavenGoal("\${project.groupId}:\${project.artifactId}:\${project.version}:update")
    @MavenOption(value = "--settings", parameter = "settings.xml")
    fun jgitProviderIsSet_scmConfigIsSet(result: MavenExecutionResult) {
        val branchList = repo.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()
            .stream()
            .map { it.name }
            .collect(toList())

        mavenAssertThat(result)
            .describedAs("the build should have been successful")
            .isSuccessful()

        assertThat(branchList)
            .describedAs("should create remote feature branches for two dependencies")
            .filteredOn { it.startsWith("refs/remotes/") }
            .filteredOn { !it.contains("origin/main") }
            .hasSize(2)
    }
}
