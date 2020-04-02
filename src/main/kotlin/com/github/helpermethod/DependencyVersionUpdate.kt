package com.github.helpermethod

import org.jsoup.nodes.Document

class DependencyVersionUpdate(
        groupId: String,
        artifactId: String,
        version: String,
        latestVersion: String,

        private val pom: Document
) : VersionUpdate(groupId, artifactId, version, latestVersion) {

    override fun update() : Unit {
        pom.selectFirst("project > dependencies > dependency:has(> groupId:containsOwn(${groupId})):has(> artifactId:containsOwn(${artifactId})):has(> version:containsOwn(${version})) > version")
                .text(latestVersion)
    }
}
