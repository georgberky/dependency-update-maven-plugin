package com.github.helpermethod

import java.nio.file.Path

class NativeGitProvider(val localRepositoryDirectory: Path) : GitProvider {
    //TODO: find the right path to git

    override fun hasRemoteBranch(branchName: String): Boolean {
        TODO("Not yet implemented")
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

    override fun push(branchName: String) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}