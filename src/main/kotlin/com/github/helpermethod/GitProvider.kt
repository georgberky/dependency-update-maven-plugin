package com.github.helpermethod

interface GitProvider : AutoCloseable {

    fun hasRemoteBranch(branchName: String) : Boolean

    fun checkoutNewBranch(branchName: String)
    fun add(filePattern: String)
    fun commit(author: String, message: String)
    fun push(branchName: String)
}