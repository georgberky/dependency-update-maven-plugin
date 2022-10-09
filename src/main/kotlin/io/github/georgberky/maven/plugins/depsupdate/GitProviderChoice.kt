package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.settings.Settings
import java.nio.file.Path

enum class GitProviderChoice {
    NATIVE {
        override fun createProvider(localRepositoryPath: Path, settings: Settings, connection: String): GitProvider =
            NativeGitProvider(localRepositoryPath)
    },
    JGIT {
        override fun createProvider(localRepositoryPath: Path, settings: Settings, connection: String): GitProvider =
            JGitProvider(localRepositoryPath, settings, connection)
    };

    abstract fun createProvider(localRepositoryPath: Path, settings: Settings, connection: String): GitProvider
}
