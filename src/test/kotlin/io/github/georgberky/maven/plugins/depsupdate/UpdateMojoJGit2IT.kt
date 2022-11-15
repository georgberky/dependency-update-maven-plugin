package io.github.georgberky.maven.plugins.depsupdate

import com.soebes.itf.jupiter.extension.*
import com.soebes.itf.jupiter.maven.MavenExecutionResult
import com.soebes.itf.jupiter.maven.MavenProjectResult
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.stream.Collectors.toList
import com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat as mavenAssertThat

@MavenJupiterExtension
@MavenProject("jgitProviderIsSet_scmConfigIsSet")
internal class UpdateMojoJGit2IT {

    @TempDir
    lateinit var remoteRepo: File

    lateinit var repo: Git

    @BeforeEach
    internal fun setUp(result: MavenProjectResult) {
        FileUtils.copyDirectory(result.targetProjectDirectory, remoteRepo)

        repo = Git.init().setDirectory(remoteRepo).call()
        repo.add().addFilepattern(".").call();
        repo.commit()
            .setAuthor("Schorsch", "georg@email.com")
            .setMessage("Initial commit.")
            .call()

        FileUtils.deleteDirectory(result.targetProjectDirectory)

        repo = Git.cloneRepository()
            .setURI(remoteRepo.toURI().toString())
            .setDirectory(result.targetProjectDirectory)
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
            .hasSize(2);

    }
}