package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec
import org.apache.maven.model.Dependency

fun ArtifactFactory.createDependencyArtifact(dependency: Dependency): Artifact =
    createDependencyArtifact(dependency.groupId, dependency.artifactId, createFromVersionSpec(dependency.version), dependency.type, dependency.classifier, dependency.scope, dependency.optional)
