package com.github.helpermethod

import com.soebes.itf.jupiter.extension.*
import com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat as mavenAssertThat
import com.soebes.itf.jupiter.maven.MavenExecutionResult
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.stream.Collectors.toList

@MavenJupiterExtension
@MavenProject("nativeProviderIsSet")
internal class UpdateMojoNativeGitIT {
    //TODO: add test for making connectionUrl or developerConnection mandatory only if provider == JGIT
    lateinit var repo : Git

    @BeforeEach
    internal fun setUp() {
        val baseDir = File(System.getProperty("basedir", System.getProperty("user.dir", ".")), "target/maven-it")
        val repoPath = File(baseDir, this.javaClass.name.replace('.', '/') + "/nativeProviderIsSet/project")

        repo = Git.init()
                .setDirectory(repoPath)
                .call()

        repo.add().addFilepattern(".").call()
        repo.commit().setAuthor("Schorsch", "georg@email.com").setMessage("initial commit").call()

        assertThat(repo.status().call().isClean).isTrue()
    }

    @MavenTest
    @MavenGoal("\${project.groupId}:\${project.artifactId}:\${project.version}:update")
    fun nativeProviderIsSet(result: MavenExecutionResult) {
        mavenAssertThat(result).isSuccessful()
        val branchList = repo.branchList().call().stream().map { it.name }.collect(toList())

        assertThat(branchList)
                .describedAs("feature branch for junit-jupiter should be created")
                .filteredOn { it.startsWith("dependency-update/org.junit.jupiter-junit-jupiter") }
                .hasSize(1)
    }
}