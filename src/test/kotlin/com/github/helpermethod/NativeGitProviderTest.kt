package com.github.helpermethod

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class NativeGitProviderTest {

    @TempDir
    lateinit var gitDirectory : Path;
    lateinit var gitRepo: Git

    @BeforeEach
    internal fun setUp() {
        gitRepo = Git.init()
                .setDirectory(gitDirectory.toFile())
                .call()
    }

    @Test
    internal fun canAddFiles() {
        val providerUnderTest = NativeGitProvider(gitDirectory)
        val newFile = File(gitDirectory.toFile(), "newFile")
        newFile.createNewFile()

        providerUnderTest.add(newFile.name)

        assertThat(gitRepo.status().call().added)
                .containsExactly(newFile.name)
    }
}