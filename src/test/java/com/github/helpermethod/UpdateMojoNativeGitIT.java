package com.github.helpermethod;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import org.apache.maven.execution.MavenExecutionResult;

@MavenJupiterExtension
class UpdateMojoNativeGitIT {

    @MavenTest(goals = "dependency-update-maven-plugin:update")
    void nativeProviderIsSet(MavenExecutionResult result) {
    }
}