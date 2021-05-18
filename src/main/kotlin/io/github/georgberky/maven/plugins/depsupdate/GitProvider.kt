package io.github.georgberky.maven.plugins.depsupdate

interface GitProvider : AutoCloseable {

    fun hasRemoteBranch(remoteBranchName: String) : Boolean

    fun checkoutNewBranch(branchName: String)
    fun add(filePattern: String)
    fun commit(author: String, message: String)
    fun push(localBranchName: String)
}