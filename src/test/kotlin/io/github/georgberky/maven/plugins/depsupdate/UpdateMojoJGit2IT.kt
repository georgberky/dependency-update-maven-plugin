package io.github.georgberky.maven.plugins.depsupdate

import com.jcraft.jsch.Session
import com.soebes.itf.jupiter.extension.MavenGoal
import com.soebes.itf.jupiter.extension.MavenJupiterExtension
import com.soebes.itf.jupiter.extension.MavenProject
import com.soebes.itf.jupiter.extension.MavenTest
import com.soebes.itf.jupiter.maven.MavenExecutionResult
import com.soebes.itf.jupiter.maven.MavenProjectResult
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.stream.Collectors.toList
import com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat as mavenAssertThat


@MavenJupiterExtension
@MavenProject("jgitProviderIsSet_scmConfigIsSet")
internal class UpdateMojoJGit2IT {

    lateinit var repo: Git

    @BeforeEach
    internal fun setUp(result: MavenProjectResult) {
        val uri = "ssh://git@localhost:2222/srv/git/jgit-test.git"
        FileUtils.deleteDirectory(result.targetProjectDirectory)

        val sshSessionFactory = object: JschConfigSessionFactory() {
            override fun configure(host: OpenSshConfig.Host?, session: Session?) {
                session?.setPassword("12345")
            }

        }
        repo = Git.cloneRepository()
            .setURI(uri)
            .setDirectory(result.targetProjectDirectory)
            .setTransportConfigCallback({ transport ->
            val sshTransport = transport as SshTransport
            sshTransport.sshSessionFactory = sshSessionFactory
        })
            .call()
    }

    @MavenTest
    @MavenGoal("\${project.groupId}:\${project.artifactId}:\${project.version}:update")
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
            .filteredOn { !it.contains("origin/master") }
            .hasSize(2)

    }
}