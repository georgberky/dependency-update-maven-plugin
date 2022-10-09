package io.github.georgberky.maven.plugins.depsupdate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ParentVersionUpdateTest {

    @Test
    fun updatedPom() {
        val pom = readPom("src/test/resources/poms/parentPom.xml")

        val updateVersionUnderTest = ParentVersionUpdate("dummy", "dummy", "dummy", "0.7.0", pom)

        val updatedPom = updateVersionUnderTest.updatedPom()

        assertThat(extractFromPom(updatedPom, "project.parent.version")).isEqualTo("0.7.0")
    }
}
