package com.github.helpermethod

import org.apache.commons.io.IOUtils
import java.nio.file.Path

class NativeGitProvider(val localRepositoryDirectory: Path) : GitProvider {
    //TODO: find the right path to git command

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        val process = ProcessBuilder("git", "branch", "--all")
                .directory(localRepositoryDirectory.toFile())
                .start()

        val returnValue = process.waitFor()
        //TODO: handle non-zero return values

        val processOutput = IOUtils.toString(process.inputStream)
        return processOutput.contains("remotes/origin/" + remoteBranchName)
    }

    override fun checkoutNewBranch(newBranchName: String) {
        val process = ProcessBuilder("git", "checkout", "-b", newBranchName)
                .directory(localRepositoryDirectory.toFile())
                .start()

        val returnValue = process.waitFor()

        //TODO: handle non-zero return values

    }

    override fun add(filePattern: String) {
        val process = ProcessBuilder("git", "add", filePattern)
                .directory(localRepositoryDirectory.toFile())
                .start()

        val returnValue = process.waitFor()

        //TODO: handle non-zero return values
    }

    override fun commit(author: String, message: String) {
        val process = ProcessBuilder("git", "commit", "-m", message, "--author='${author}'")
                .directory(localRepositoryDirectory.toFile())
                .start()

        val returnValue = process.waitFor()

        //TODO: handle non-zero return values
    }

    override fun push(localBranchName: String) {
       val process = ProcessBuilder("git", "push", "--set-upstream", "origin", localBranchName)
               .directory(localRepositoryDirectory.toFile())
               .start()

        val returnValue = process.waitFor()

        //TODO: handle non-zero return values

    }

    override fun close() {
        // no needed for native git
    }
}