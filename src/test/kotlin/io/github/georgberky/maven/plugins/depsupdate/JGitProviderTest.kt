package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.settings.Settings
import org.assertj.core.api.Assumptions.assumeThatCode
import org.eclipse.jgit.api.errors.TransportException
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class JGitProviderTest : GitProviderTest() {
    override fun setupGitProvider(localGitDirectoryPath: Path): GitProvider {
        return JGitProvider(localGitDirectoryPath, Settings(), "scm:git:"+remoteGitDirectory.toURI().toString())
    }

    @Test // TODO need a solution for testing push with JGitProvider
    internal fun `push change to remote repository`() {
        val fileToCommit = File(localGitDirectory, "fileToCommit")
        fileToCommit.createNewFile()
        localGitRepo.branchCreate().setName("newBranch").call()
        localGitRepo.checkout().setName("newBranch").call()
        localGitRepo.add().addFilepattern(fileToCommit.name).call()
        localGitRepo.commit().setAuthor("Georg", "georg@email.com").setMessage("init").call()

        val lambda = { providerUnderTest.push("newBranch") }

        assumeThatCode(lambda).isInstanceOf(TransportException::class.java)
    }
}