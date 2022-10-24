package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.plugin.logging.Log

interface BranchUpdateStrategy {
    fun handle(branchName: UpdateMojo.UpdateBranchName, log: Log)
}

