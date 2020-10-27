package com.github.helpermethod

import com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat
import com.soebes.itf.jupiter.extension.MavenGoal
import com.soebes.itf.jupiter.extension.MavenGoals
import com.soebes.itf.jupiter.extension.MavenJupiterExtension
import com.soebes.itf.jupiter.extension.MavenTest
import com.soebes.itf.jupiter.maven.MavenExecutionResult

@MavenJupiterExtension
internal class UpdateMojoNativeGitIT {
    //TODO: add test for making connectionUrl or developerConnection mandatory only if provider == JGIT
    @MavenTest
    @MavenGoal("\${project.groupId}:\${project.artifactId}:\${project.version}:update")
    fun nativeProviderIsSet(result: MavenExecutionResult) {
        assertThat(result).isSuccessful()
    }
}