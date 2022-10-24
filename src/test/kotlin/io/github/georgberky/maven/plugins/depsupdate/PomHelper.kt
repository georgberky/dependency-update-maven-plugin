package io.github.georgberky.maven.plugins.depsupdate

import io.restassured.path.xml.XmlPath
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.File

fun readPom(pomPath: String): Document =
    Jsoup.parse(File(pomPath).readText(), "", Parser.xmlParser()).apply {
        outputSettings().prettyPrint(false)
    }

fun extractFromPom(pom: Document, xmlPath: String) =
    XmlPath.with(pom.html()).getString(xmlPath)
