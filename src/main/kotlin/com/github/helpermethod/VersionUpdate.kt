package com.github.helpermethod

import org.jsoup.nodes.Document

abstract class VersionUpdate(
        val groupId: String,
        val artifactId: String,
        protected val version: String,
        val latestVersion: String
) {
    abstract fun updatedPom() : Document

    fun canSkip() = latestVersion == version
}