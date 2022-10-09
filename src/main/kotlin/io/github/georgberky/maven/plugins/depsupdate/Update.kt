package io.github.georgberky.maven.plugins.depsupdate

import org.jsoup.nodes.Document

data class Update(val groupId: String, val artifactId: String, val version: String, val latestVersion: String, val modification: (Document) -> Unit)
