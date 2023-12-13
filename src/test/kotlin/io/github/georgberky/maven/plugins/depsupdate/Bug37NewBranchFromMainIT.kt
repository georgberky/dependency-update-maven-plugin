package io.github.georgberky.maven.plugins.depsupdate

import com.soebes.itf.jupiter.extension.MavenGoal
import com.soebes.itf.jupiter.extension.MavenJupiterExtension
import com.soebes.itf.jupiter.extension.MavenTest
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

@MavenJupiterExtension
internal class Bug37NewBranchFromMainIT {

    @TempDir
    lateinit var remoteRepo: File

    lateinit var repo: Git

    @BeforeEach
    internal fun setUp(result: MavenProjectResult) {
        FileUtils.copyDirectory(result.targetProjectDirectory, remoteRepo)

        repo = Git.init().setInitialBranch("main").setDirectory(remoteRepo).call()
        repo.add().addFilepattern(".").call()
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
    fun onlyOneDependency(result: MavenExecutionResult) {
        val branchList = repo.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()
            .stream()
            .map { it.name }
            .collect(toList())

        val updatedBranchName = branchList.filter { it.startsWith("refs/heads/dependency-update") }.first()
        repo.checkout().setName(updatedBranchName).call()

        val previousCommit = repo.repository.resolve("HEAD^")

        val branchesWithPreviousCommit = repo.branchList().setContains(previousCommit.name()).call()
            .map { it.name }
            .toList()

        assertThat(branchesWithPreviousCommit).contains("refs/heads/main")
    }

    @MavenTest
    @MavenGoal("\${project.groupId}:\${project.artifactId}:\${project.version}:update")
    fun moreThanOneDependency(result: MavenExecutionResult) {
        val branchList = repo.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()
            .stream()
            .map { it.name }
            .collect(toList())

        val updatedBranchNames = branchList.filter { it.startsWith("refs/heads/dependency-update") }

        updatedBranchNames.forEach {
            repo.checkout().setName(it).call()

            val previousCommit = repo.repository.resolve("HEAD^")

            val branchesWithPreviousCommit = repo.branchList().setContains(previousCommit.name()).call()
                .map { it.name }
                .toList()

            assertThat(branchesWithPreviousCommit).contains("refs/heads/main")
        }
    }
}
