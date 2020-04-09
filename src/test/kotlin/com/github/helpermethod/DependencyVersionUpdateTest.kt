package com.github.helpermethod

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class DependencyVersionUpdateTest {

    @Test
    fun updatedPom() {
        val pom = readPom("src/test/resources/poms/dependenciesPom.xml")

        val updateVersionUnderTest = DependencyVersionUpdate("org.junit.jupiter", "junit-jupiter", "5.5.2", "5.6.0", pom)

        val updatedPom = updateVersionUnderTest.updatedPom()

        assertThat(extractFromPom(updatedPom, "project.dependencies.dependency.find{ it.artifactId == 'junit-jupiter' }.version")).isEqualTo("5.6.0")
        assertThat(extractFromPom(updatedPom, "project.dependencies.dependency.find{ it.artifactId == 'assertj-core' }.version")).isEqualTo("3.15.0")

    }
}