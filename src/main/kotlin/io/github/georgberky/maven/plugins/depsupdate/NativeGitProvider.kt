package io.github.georgberky.maven.plugins.depsupdate

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Path

open class NativeGitProvider(val localRepositoryDirectory: Path) : GitProvider {

    private val gitCommand: String by lazy {
        val process = ProcessBuilder("which", "git")
            .start()

        process.waitFor()
        IOUtils.toString(process.inputStream, StandardCharsets.UTF_8).trim()
    }

    private val initialBranch: String =
        run(gitCommand, "rev-parse", "--abbrev-ref", "HEAD").orThrow().stdout.trim()

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        val processResult = run(gitCommand, "branch", "--all").orThrow()
        val processOutput = processResult.stdout
        return processOutput.contains("remotes/origin/" + remoteBranchName)
    }

    override fun hasRemoteBranchWithPrefix(remoteBranchNamePrefix: String): Boolean {
        val processResult = run(gitCommand, "branch", "--all").orThrow()
        val processOutput = processResult.stdout
        return processOutput.lines()
            .map { it.trim() }
            .any { it.startsWith("remotes/origin/" + remoteBranchNamePrefix) }
    }

    override fun checkoutNewBranch(branchName: String) {
        run(gitCommand, "checkout", "-b", branchName).orThrow()
    }

    override fun add(filePattern: String) {
        run(gitCommand, "add", filePattern).orThrow()
    }

    override fun commit(author: String, email: String, message: String) {
        run(gitCommand, "commit", "-m", message, "--author='$author <$email>'").orThrow()
    }

    override fun push(localBranchName: String) {
        run(gitCommand, "push", "--set-upstream", "origin", localBranchName).orThrow()
    }

    override fun checkoutInitialBranch() {
        run(gitCommand, "checkout", initialBranch).orThrow()
    }

    override fun close() {
        // not needed for native git
    }

    open fun run(vararg command: String): ProcessResult {
        val process = ProcessBuilder(*command)
            .directory(localRepositoryDirectory.toFile())
            .start()

        val exitCode = process.waitFor()
        val stdout = IOUtils.toString(process.inputStream, StandardCharsets.UTF_8)
        val stderr = IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)

        return ProcessResult(command, exitCode, stdout, stderr)
    }

    data class ProcessResult(
        val command: Array<out String>,
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        fun orThrow(): ProcessResult {
            if (exitCode == 0) {
                return this
            }
            throw ProcessException(
                "Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $exitCode\n" +
                    "stdout was: $stdout\n" +
                    "stderr was: $stderr"
            )
        }
    }

    class ProcessException(message: String?) : RuntimeException(message)
}
