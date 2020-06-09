package com.github.helpermethod

import org.apache.commons.io.IOUtils
import java.nio.file.Path

class NativeGitProvider(val localRepositoryDirectory: Path) : GitProvider {
    //TODO: find the right path to git command

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        val processResult = runInProcessWithOutput("git", "branch", "--all")

        val returnValue = processResult.first
        val processOutput = processResult.second

        return processOutput.contains("remotes/origin/" + remoteBranchName)
    }

    override fun checkoutNewBranch(newBranchName: String) {
        val returnValue = runInProcess("git", "checkout", "-b", newBranchName)

        //TODO: handle non-zero return values

    }

    override fun add(filePattern: String) {
        val returnValue = runInProcess("git", "add", filePattern)

        //TODO: handle non-zero return values
    }

    override fun commit(author: String, message: String) {
        val result = runInProcess("git", "commit", "-m", message, "--author='${author}'")

        //TODO: handle non-zero return values
    }

    override fun push(localBranchName: String) {
        val returnValue = runInProcess("git", "push", "--set-upstream", "origin", localBranchName)

        //TODO: handle non-zero return values

    }

    override fun close() {
        // no needed for native git
    }

    private fun runInProcess(vararg command: String): Int {
        return runInProcessWithOutput(*command).first
    }

    fun runInProcessWithOutput(vararg command: String): Pair<Int, String> {
        val process = ProcessBuilder(*command)
                .directory(localRepositoryDirectory.toFile())
                .start()

        val returnValue = process.waitFor()
        val processOutput = IOUtils.toString(process.inputStream)

        return Pair(returnValue, processOutput)
    }
}