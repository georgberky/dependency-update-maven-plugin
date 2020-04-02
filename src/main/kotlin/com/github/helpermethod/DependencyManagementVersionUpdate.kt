package com.github.helpermethod

import org.jsoup.nodes.Document

class DependencyManagementVersionUpdate(
        groupId: String,
        artifactId: String,
        version: String,
        latestVersion: String,
        private val pom: Document
) : VersionUpdate(groupId, artifactId, version, latestVersion) {

    override fun update() : Unit {
        pom.selectFirst("project > dependencyManagement > dependencies > dependency:has(> groupId:containsOwn(${groupId})):has(> artifactId:containsOwn(${artifactId})):has(> version:containsOwn(${version})) > version")
                .text(latestVersion)
    }
}
