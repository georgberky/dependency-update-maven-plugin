package com.github.helpermethod

import org.assertj.core.util.Maps
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.xmlunit.assertj.XmlAssert
import java.io.File

fun assertThatPom(pom: Document) = XmlAssert.assertThat(pom.html())
        .withNamespaceContext(Maps.newHashMap("pom", "http://maven.apache.org/POM/4.0.0"))

fun readPom(pomPath: String): Document =
        Jsoup.parse(File(pomPath).readText(), "", Parser.xmlParser()).apply {
            outputSettings().prettyPrint(false)
        }