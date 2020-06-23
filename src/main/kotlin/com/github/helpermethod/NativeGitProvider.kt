package com.github.helpermethod

import org.apache.commons.io.IOUtils
import java.nio.file.Path

open class NativeGitProvider(val localRepositoryDirectory: Path) : GitProvider {
    //TODO: find the right path to git command

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        val command = arrayOf("git", "branch", "--all")
        val processResult = runInProcessWithOutput("git", "branch", "--all")

        val returnValue = processResult.first
        if(returnValue != 0) {
            throw ProcessException("Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $returnValue")
        }


        val processOutput = processResult.second
        return processOutput.contains("remotes/origin/" + remoteBranchName)
    }

    override fun checkoutNewBranch(newBranchName: String) {
        val command = arrayOf("git", "checkout", "-b", newBranchName)
        val returnValue = runInProcess(*command)

        if(returnValue != 0) {
            throw ProcessException("Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $returnValue")
        }
    }

    override fun add(filePattern: String) {
        val command = arrayOf("git", "add", filePattern)
        val returnValue = runInProcess(*command)

        if(returnValue != 0) {
            throw ProcessException("Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $returnValue")
        }
    }

    override fun commit(author: String, message: String) {
        val command = arrayOf("git", "commit", "-m", message, "--author='${author}'")
        val returnValue = runInProcess(*command)

        if(returnValue != 0) {
            throw ProcessException("Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $returnValue")
        }
    }

    override fun push(localBranchName: String) {
        val command = arrayOf("git", "push", "--set-upstream", "origin", localBranchName)
        val returnValue = runInProcess(*command)

        if(returnValue != 0) {
            throw ProcessException("Native git invocation failed. " +
                    "Command: ${command.joinToString(" ")}, " +
                    "return value was: $returnValue")
        }

    }

    override fun close() {
        // no needed for native git
    }

    private fun runInProcess(vararg command: String): Int {
        return runInProcessWithOutput(*command).first
    }

    open fun runInProcessWithOutput(vararg command: String): Pair<Int, String> {
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