package io.github.georgberky.maven.plugins.depsupdate

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.nio.file.Path

abstract class GitProviderTest() {
    @TempDir
    lateinit var tempDir: File
    lateinit var localGitDirectory: File
    lateinit var localGitRepo: Git
    lateinit var remoteGitDirectory: File
    lateinit var remoteGitRepo: Git
    lateinit var providerUnderTest: GitProvider

    @BeforeEach
    internal fun setUp() {
        setupRemoteGitRepoWithInitCommit()
        setupLocalGitRepoAsCloneOf(remoteGitDirectory.toURI())

        providerUnderTest = setupGitProvider(localGitDirectory.toPath())
    }

    abstract fun setupGitProvider(localGitDirectoryPath: Path) : GitProvider

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

        providerUnderTest.commit("Sandra Parsick","sandra@email.com", "Hello George")

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
    internal fun `has no remote branch`() {
        val hasRemoteBranch = providerUnderTest.hasRemoteBranch("remoteBranch")

        assertThat(hasRemoteBranch).isFalse();
    }

    @Test
    internal fun `has remote branch`() {
        remoteGitRepo.branchCreate().setName("myNewRemoteBranch").call()
        localGitRepo.fetch().call()

        val hasRemoteBranch = providerUnderTest.hasRemoteBranch("myNewRemoteBranch");

        assertThat(hasRemoteBranch)
            .describedAs("remote branch should exist")
            .isTrue()
    }

    @Test
    internal fun `checkout initial branch`() {
        val initialBranch = localGitRepo.repository.branch
        val fileToCommit = File(localGitDirectory, "fileToCommit")
        fileToCommit.createNewFile()
        localGitRepo.branchCreate().setName("newBranch").call()
        localGitRepo.checkout().setName("newBranch").call()
        localGitRepo.add().addFilepattern(fileToCommit.name).call()
        localGitRepo.commit().setAuthor("Georg", "georg@email.com").setMessage("init").call()

        providerUnderTest.checkoutInitialBranch()

        assertThat(localGitRepo.repository.branch).isEqualTo(initialBranch)

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