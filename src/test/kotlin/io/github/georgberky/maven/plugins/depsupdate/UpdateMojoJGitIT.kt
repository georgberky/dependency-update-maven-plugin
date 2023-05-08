package io.github.georgberky.maven.plugins.depsupdate

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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
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
    var gitServer = GenericContainer(DockerImageName.parse("rockstorm/git-server:2.38"))
        .withEnv("GIT_PASSWORD", "12345")
        .withExposedPorts(22)
        .waitingFor(Wait.forLogMessage(".*Container configuration completed.*", 1))

    @TempDir
    lateinit var remoteRepo: File

    lateinit var repo: Git

    @BeforeEach
    internal fun setUp(result: MavenProjectResult) {
        gitServer.execInContainer("mkdir", "-p", "/srv/git/jgit-test.git")
        gitServer.execInContainer("git", "config", "--global", "init.defaultBranch", "main")
        gitServer.execInContainer("git", "init", "--bare", "/srv/git/jgit-test.git")
        gitServer.execInContainer("chown", "-R", "git:git", "/srv")

        val gitPort = gitServer.getMappedPort(22)

        val sshSessionFactory = object : JschConfigSessionFactory() {
            override fun configure(host: OpenSshConfig.Host?, session: Session?) {
                session?.setPassword("12345")
                session?.setConfig("StrictHostKeyChecking", "no")
            }
        }

        // TODO: replace scm connection in pom.xml
        val remoteRepoGit = Git.cloneRepository()
            .setURI("ssh://git@localhost:$gitPort/srv/git/jgit-test.git")
            .setDirectory(remoteRepo)
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

        FileUtils.deleteDirectory(result.targetProjectDirectory)

        repo = Git.cloneRepository()
            .setURI("ssh://git@localhost:$gitPort/srv/git/jgit-test.git")
            .setDirectory(result.targetProjectDirectory)
            .setBranch("main")
            .setTransportConfigCallback({ transport ->
                val sshTransport = transport as SshTransport
                sshTransport.sshSessionFactory = sshSessionFactory
            })
            .call()
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
