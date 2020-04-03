package com.github.helpermethod

abstract class VersionUpdate(
        val groupId: String,
        val artifactId: String,
        protected val version: String,
        val latestVersion: String
) {
    abstract fun update() : Unit

    fun canSkip() = latestVersion == version
}