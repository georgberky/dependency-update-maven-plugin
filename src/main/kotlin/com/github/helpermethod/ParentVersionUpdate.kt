package com.github.helpermethod

import org.jsoup.nodes.Document

class ParentVersionUpdate(
        groupId: String,
        artifactId: String,
        version: String,

        latestVersion: String,
        private val pom: Document
) : VersionUpdate(groupId, artifactId, version, latestVersion) {

    override fun update() : Unit {
        pom.selectFirst("project > parent > version").text(latestVersion)
    }
}