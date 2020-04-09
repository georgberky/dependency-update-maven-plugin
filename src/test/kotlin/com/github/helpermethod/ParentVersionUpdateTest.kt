package com.github.helpermethod

import org.junit.jupiter.api.Test

internal class ParentVersionUpdateTest {

    @Test
    fun updatedPom() {
        val pom = readPom("src/test/resources/poms/parentPom.xml")

        val updateVersionUnderTest = ParentVersionUpdate("dummy", "dummy", "dummy", "0.7.0", pom)

        val updatedPom = updateVersionUnderTest.updatedPom()

        assertThatPom(updatedPom)
                .valueByXPath("//pom:project/pom:parent/pom:version/text()")
                .isEqualTo("0.7.0")
    }
}