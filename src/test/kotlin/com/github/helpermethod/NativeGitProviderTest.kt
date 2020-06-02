package com.github.helpermethod

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

class NativeGitProviderTest {

    @TempDir
    lateinit var tempDir: File

    lateinit var localGitDirectory : File
    lateinit var localGitRepo: Git

    lateinit var remoteGitDirectory : File
    lateinit var remoteGitRepo: Git

    lateinit var providerUnderTest: NativeGitProvider

    @BeforeEach
    internal fun setUp() {
        remoteGitDirectory = File (tempDir, "remoteGitRepoD")
        remoteGitRepo = Git.init()
                .setDirectory(remoteGitDirectory)
                .call()
        val fileToCommit = File(remoteGitDirectory, "initFile")
        fileToCommit.createNewFile()
        remoteGitRepo.add().addFilepattern(fileToCommit.name).call()
        remoteGitRepo.commit().setAuthor("Georg", "georg@email.com").setMessage("init").call()

        localGitDirectory = File(tempDir, "localGitRepoDir")
        localGitRepo = Git.cloneRepository()
                .setURI(remoteGitDirectory.toURI().toString())
                .setDirectory(localGitDirectory)
                .call()

        providerUnderTest = NativeGitProvider(localGitDirectory.toPath())
    }

    @Test
    @DisplayName("can add files")
    internal fun canAddFiles() {
        val newFile = File(localGitDirectory, "newFile")
        newFile.createNewFile()

        providerUnderTest.add(newFile.name)

        assertThat(localGitRepo.status().call().added)
                .containsExactly(newFile.name)
    }

    @Test
    @DisplayName("can commit")
    internal fun canCommit() {
        val fileToCommit = File(localGitDirectory, "fileToCommit")
        fileToCommit.createNewFile()
        localGitRepo.add().addFilepattern(fileToCommit.name).call()

        providerUnderTest.commit("Sandra Parsick <sandra@email.com>", "Hello George")

        assertThat(localGitRepo.status().call().isClean)
                .describedAs("repository status should be clean")
                .isTrue()

        assertThat(localGitRepo.log().setMaxCount(1).call().first())
                .describedAs("commit author and message should match")
                .extracting("authorIdent.name", "authorIdent.emailAddress", "shortMessage")
                .isEqualTo(listOf("Sandra Parsick", "sandra@email.com", "Hello George"))
    }

    @Test
    @DisplayName("can checkout")
    internal fun canCheckoutNewBranch() {
        providerUnderTest.checkoutNewBranch("newBranch")

        assertThat(localGitRepo.repository.branch).isEqualTo("newBranch")
    }

    @Test
    @DisplayName("has no remote branch")
    internal fun hasNoRemoteBranch(){
        val hasRemoteBranch = providerUnderTest.hasRemoteBranch("remoteBranch")

        assertThat(hasRemoteBranch).isFalse();
    }

    @Test
    @DisplayName("has remote branch")
    internal fun hasRemoteBranch(){
        remoteGitRepo.branchCreate().setName("myNewRemoteBranch").call()
        localGitRepo.fetch().call()

        val hasRemoteBranch = providerUnderTest.hasRemoteBranch("myNewRemoteBranch");

        assertThat(hasRemoteBranch).isTrue();
    }
}