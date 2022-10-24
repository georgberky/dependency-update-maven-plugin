package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.plugin.logging.Log

class WarnOnlyBranchUpdateStrategy : BranchUpdateStrategy {
    override fun handle(branchName: UpdateMojo.UpdateBranchName, log: Log) {
        log.warn(
            "Dependency '${branchName.prefix()}' already has a branch for a previous version update. " +
                "Please merge it first"
        )
    }
}