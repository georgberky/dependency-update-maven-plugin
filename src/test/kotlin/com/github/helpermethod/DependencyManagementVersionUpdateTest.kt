package com.github.helpermethod

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class DependencyManagementVersionUpdateTest {

    @Test
    fun updatedPom() {
        val pom = readPom("src/test/resources/poms/dependencyManagementPom.xml")

        val updateVersionUnderTest = DependencyManagementVersionUpdate("org.junit.jupiter", "junit-jupiter", "5.5.2", "5.6.0", pom)

        val updatedPom = updateVersionUnderTest.updatedPom()

        assertThat(extractFromPom(updatedPom, "project.dependencyManagement.dependencies.dependency.find{ it.artifactId == 'junit-jupiter' }.version")).isEqualTo("5.6.0")
        assertThat(extractFromPom(updatedPom, "project.dependencyManagement.dependencies.dependency.find{ it.artifactId == 'assertj-core' }.version")).isEqualTo("3.15.0")
    }
}