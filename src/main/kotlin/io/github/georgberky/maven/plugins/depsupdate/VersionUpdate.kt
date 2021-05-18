package io.github.georgberky.maven.plugins.depsupdate

import org.jsoup.nodes.Document

abstract class VersionUpdate(
        val groupId: String,
        val artifactId: String,
        val version: String,
        val latestVersion: String
) {
    abstract fun updatedPom() : Document

    fun canSkip() : Boolean {
        println("canSkip: ${latestVersion} compare to ${version}")
        println("latestVersion.equals(version) = ${latestVersion.equals(version)}")
        return !latestVersion.equals(version)
    }

}