package io.github.georgberky.maven.plugins.depsupdate

enum class BranchUpdateStrategyChoice {
    WARN {
        override fun create(): BranchUpdateStrategy {
            return WarnOnlyBranchUpdateStrategy()
        }
    },
    UPDATE {
        override fun create(): BranchUpdateStrategy {
            TODO("Not yet implemented")
        }
    };

    abstract fun create(): BranchUpdateStrategy
}
