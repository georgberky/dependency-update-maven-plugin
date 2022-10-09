package io.github.georgberky.maven.plugins.depsupdate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.util.stream.Collectors

class NativeGitProviderTest : GitProviderTest() {
    override fun setupGitProvider(localGitDirectoryPath: Path): GitProvider = NativeGitProvider(localGitDirectoryPath)

    @Test // TODO need a solution for testing push with JGitProvider
    internal fun `push change to remote repository`() {
        val fileToCommit = File(localGitDirectory, "fileToCommit")
        fileToCommit.createNewFile()
        localGitRepo.branchCreate().setName("newBranch").call()
        localGitRepo.checkout().setName("newBranch").call()
        localGitRepo.add().addFilepattern(fileToCommit.name).call()
        localGitRepo.commit().setAuthor("Georg", "georg@email.com").setMessage("init").call()

        providerUnderTest.push("newBranch")

        val branchList = remoteGitRepo.branchList().call().stream()
            .map { it.name }
            .collect(Collectors.toList())
        Assertions.assertThat(branchList).contains("refs/heads/newBranch")
    }
}
