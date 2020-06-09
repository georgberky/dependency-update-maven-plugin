package com.github.helpermethod

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class NativeGitProviderErrorHandlingTest {

    private var processOutput: String = ""
    private var returnValue: Int = 0
    private lateinit var gitProvider: NativeGitProvider

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    internal fun setUp() {
        gitProvider = object : NativeGitProvider(tempDir.toPath()) {
            override fun runInProcessWithOutput(vararg command: String): Pair<Int, String> {
                return Pair(returnValue, processOutput)
            }
        }
    }

    @Test
    internal fun `error handling`() {
        returnValue = -1

        val callAdd = { gitProvider.add("") }

        assertThatThrownBy(callAdd)
                .isInstanceOf(NativeGitProvider.ProcessException::class.java)
                .hasMessageContaining("return value")
                .hasMessageContaining("-1")
                .hasMessageContaining("git add")
    }
}

