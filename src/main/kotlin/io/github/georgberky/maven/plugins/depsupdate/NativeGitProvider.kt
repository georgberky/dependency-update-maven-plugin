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
        runInProcessWithOutput(gitCommand, "rev-parse", "--abbrev-ref", "HEAD").stdout.trim()

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        val processResult = runInProcessWithOutput(gitCommand, "branch", "--all")
        val processOutput = processResult.stdout
        return processOutput.contains("remotes/origin/" + remoteBranchName)
    }

    override fun hasRemoteBranchWithPrefix(remoteBranchNamePrefix: String): Boolean {
        val processResult = runInProcessWithOutput(gitCommand, "branch", "--all")
        val processOutput = processResult.stdout
        return processOutput.lines()
            .map { it.trim() }
            .any { it.startsWith("remotes/origin/" + remoteBranchNamePrefix) }
    }

    override fun checkoutNewBranch(branchName: String) {
        runInProcess(gitCommand, "checkout", "-b", branchName)
    }

    override fun add(filePattern: String) {
        runInProcess(gitCommand, "add", filePattern)
    }

    override fun commit(author: String, email: String, message: String) {
        runInProcess(gitCommand, "commit", "-m", message, "--author='$author <$email>'")
    }

    override fun push(localBranchName: String) {
        runInProcess(gitCommand, "push", "--set-upstream", "origin", localBranchName)
    }

    override fun checkoutInitialBranch() {
        runInProcess(gitCommand, "checkout", initialBranch)
    }

    override fun close() {
        // no needed for native git
    }

    private fun runInProcess(vararg command: String): Int {
        return runInProcessWithOutput(*command).exitCode
    }

    private fun runInProcessWithOutput(vararg command: String): ProcessResult {
        return run(*command).orThrow()
    }

    open fun run(vararg command: String): ProcessResult {
        val process = ProcessBuilder(*command)
            .directory(localRepositoryDirectory.toFile())
            .start()
        val returnValue = process.waitFor()
        val processOutput = IOUtils.toString(process.inputStream, StandardCharsets.UTF_8)
        val processErr = IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)
        val (exitCode, stdout, stderr) = Triple(returnValue, processOutput, processErr)
        return ProcessResult(command.joinToString(" "), exitCode, stdout, stderr)
    }

    data class ProcessResult(
        val command: String,
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
                    "Command: $command, " +
                    "return value was: $exitCode\n" +
                    "stdout was: $stdout\n" +
                    "stderr was: $stderr"
            )
        }
    }

    class ProcessException(message: String?) : RuntimeException(message)
}
