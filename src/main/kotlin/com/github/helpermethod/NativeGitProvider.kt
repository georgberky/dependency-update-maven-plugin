package com.github.helpermethod

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

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        val processResult = runInProcessWithOutput(gitCommand, "branch", "--all")
        val processOutput = processResult.second
        return processOutput.contains("remotes/origin/" + remoteBranchName)
    }

    override fun checkoutNewBranch(newBranchName: String) {
        runInProcess(gitCommand, "checkout", "-b", newBranchName)
    }

    override fun add(filePattern: String) {
        runInProcess(gitCommand, "add", filePattern)
    }

    override fun commit(author: String, message: String) {
        runInProcess(gitCommand, "commit", "-m", message, "--author='${author} <test@test.org>'")
    }

    override fun push(localBranchName: String) {
        runInProcess(gitCommand, "push", "--set-upstream", "origin", localBranchName)
    }

    override fun close() {
        // no needed for native git
    }

    private fun runInProcess(vararg command: String): Int {
        return runInProcessWithOutput(*command).first
    }

    private fun runInProcessWithOutput(vararg command: String): Pair<Int, String> {
        val result = run(*command)
        val returnValue = result.first

        if(returnValue != 0) {
            throw ProcessException("Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $returnValue")
        }

        return result
    }

    open fun run(vararg command: String): Pair<Int, String> {

        val process = ProcessBuilder(*command)
                .directory(localRepositoryDirectory.toFile())
                .start()

        val returnValue = process.waitFor()
        val processOutput = IOUtils.toString(process.inputStream)
        return Pair(returnValue, processOutput)
    }

    class ProcessException(message: String?) : RuntimeException(message) {

    }
}