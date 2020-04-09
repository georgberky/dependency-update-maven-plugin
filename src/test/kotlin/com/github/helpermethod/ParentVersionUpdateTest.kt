package com.github.helpermethod

import org.assertj.core.util.Maps
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.junit.jupiter.api.Test
import org.xmlunit.assertj.XmlAssert.assertThat
import java.io.File

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

    private fun assertThatPom(updatedPom: Document) = assertThat(updatedPom.html())
            .withNamespaceContext(Maps.newHashMap("pom", "http://maven.apache.org/POM/4.0.0"))

    private fun readPom(pomPath: String) : Document =
            Jsoup.parse(File(pomPath).readText(), "", Parser.xmlParser()).apply {
                outputSettings().prettyPrint(false)
            }
}