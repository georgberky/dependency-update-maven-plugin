package com.github.helpermethod

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.util.stream.Collectors.toList

@Disabled("Not working locally.")
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
        setupRemoteGitRepoWithInitCommit()
        setupLocalGitRepoAsCloneOf(remoteGitDirectory.toURI())

        providerUnderTest = NativeGitProvider(localGitDirectory.toPath())
    }

    @Test
    internal fun `can add files`() {
        val newFile = File(localGitDirectory, "newFile")
        newFile.createNewFile()

        providerUnderTest.add(newFile.name)

        assertThat(localGitRepo.status().call().added)
                .containsExactly(newFile.name)
    }

    @Test
    internal fun `can commit`() {
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
    internal fun `can checkout new branch`() {
        providerUnderTest.checkoutNewBranch("newBranch")

        assertThat(localGitRepo.repository.branch).isEqualTo("newBranch")
    }

    @Test
    internal fun `has no remote branch`(){
        val hasRemoteBranch = providerUnderTest.hasRemoteBranch("remoteBranch")

        assertThat(hasRemoteBranch).isFalse();
    }

    @Test
    internal fun `has remote branch`(){
        remoteGitRepo.branchCreate().setName("myNewRemoteBranch").call()
        localGitRepo.fetch().call()

        val hasRemoteBranch = providerUnderTest.hasRemoteBranch("myNewRemoteBranch");

        assertThat(hasRemoteBranch).isTrue();
    }

    @Test
    internal fun `push change to remote repository`(){
        val fileToCommit = File(localGitDirectory, "fileToCommit")
        fileToCommit.createNewFile()
        localGitRepo.branchCreate().setName("newBranch").call()
        localGitRepo.checkout().setName("newBranch").call()
        localGitRepo.add().addFilepattern(fileToCommit.name).call()
        localGitRepo.commit().setAuthor("Georg", "georg@email.com").setMessage("init").call()

        providerUnderTest.push("newBranch")

        val branchList = remoteGitRepo.branchList().call().stream()
                                                                                .map { it.name }
                                                                                .collect(toList())
        assertThat(branchList).contains("refs/heads/newBranch")
    }

    private fun setupLocalGitRepoAsCloneOf(remoteGitDirectory: URI) {
        localGitDirectory = File(tempDir, "localGitRepoDir")
        localGitRepo = Git.cloneRepository()
                .setURI(remoteGitDirectory.toString())
                .setDirectory(localGitDirectory)
                .call()
    }

    private fun setupRemoteGitRepoWithInitCommit() {
        remoteGitDirectory = File(tempDir, "remoteGitRepoD")
        remoteGitRepo = Git.init()
                .setDirectory(remoteGitDirectory)
                .call()
        val fileToCommit = File(remoteGitDirectory, "initFile")
        fileToCommit.createNewFile()
        remoteGitRepo.add().addFilepattern(fileToCommit.name).call()
        remoteGitRepo.commit().setAuthor("Georg", "georg@email.com").setMessage("init").call()
    }
}