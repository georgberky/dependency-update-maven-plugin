package com.github.helpermethod

import org.assertj.core.api.Assertions.assertThatThrownBy
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
    internal fun `error handling for add`() {
        returnValue = -1

        val callAdd = { gitProvider.add("") }

        assertThatThrownBy(callAdd)
                .isInstanceOf(NativeGitProvider.ProcessException::class.java)
                .hasMessageContaining("return value")
                .hasMessageContaining("-1")
                .hasMessageContaining("git add")
    }

    @Test
    internal fun `error handling for checkout new branch`(){
        returnValue = -1

        val callCheckoutNewBranch = { gitProvider.checkoutNewBranch("newBranch") }

        assertThatThrownBy(callCheckoutNewBranch)
                .isInstanceOf(NativeGitProvider.ProcessException::class.java)
                .hasMessageContaining("return value")
                .hasMessageContaining("-1")
                .hasMessageContaining("git checkout")
                .hasMessageContaining("newBranch")
    }

    @Test
    internal fun `error handling for a remote branch checking` (){
        returnValue = -1

        val callHasRemoteBranch : () -> Unit = { gitProvider.hasRemoteBranch("remoteBranch")}

        assertThatThrownBy(callHasRemoteBranch)
                .isInstanceOf(NativeGitProvider.ProcessException::class.java)
                .hasMessageContaining("return value")
                .hasMessageContaining("-1")
                .hasMessageContaining("git branch")
    }

    @Test
    internal fun `error handling for commit`(){
        returnValue = -1

        val callCommit : () -> Unit = { gitProvider.commit("Georg Berky", "a commit message")}

        assertThatThrownBy(callCommit)
                .isInstanceOf(NativeGitProvider.ProcessException::class.java)
                .hasMessageContaining("return value")
                .hasMessageContaining("-1")
                .hasMessageContaining("git commit")
                .hasMessageContaining("Georg Berky")
                .hasMessageContaining("a commit message")
    }
}

