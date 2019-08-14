package com.github.helpermethod

import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

@Mojo(name = "update-dependency")
class UpdateDependencyMojo extends AbstractMojo {
    @Parameter(defaultValue = '${project}', readonly = true)
    MavenProject mavenProject
    @Parameter(defaultValue = '${localRepository}', readonly = true)
    ArtifactRepository localRepository
    @Parameter(defaultValue = '${project.remoteArtifactRepositories}', readonly = true)
    List remoteArtifactRepositories
    @Component
    ArtifactMetadataSource artifactMetadataSource
    @Component
    ArtifactFactory artifactFactory

    void execute() throws MojoExecutionException, MojoFailureException {
        def parent = artifactFactory.createParentArtifact(mavenProject.parent.groupId, mavenProject.parent.artifactId, mavenProject.parent.version)
        def versions = artifactMetadataSource.retrieveAvailableVersions(parent, localRepository, remoteArtifactRepositories)
        def latestParentVersion = versions.max().toString()
        def modifiedPom = new XmlParser(false, false).parse(mavenProject.file).tap {
            children().find { it.name() == 'parent' }.version[0].value = latestParentVersion
        }

        mavenProject.file.withPrintWriter {
            new XmlNodePrinter(it, " " * 4).tap {
                preserveWhitespace = true
            }.print(modifiedPom)
        }
    }
}