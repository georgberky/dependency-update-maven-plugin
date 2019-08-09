package com.github.helpermethod

import groovy.transform.CompileStatic
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.artifact.metadata.ArtifactMetadataSource

@CompileStatic
@Mojo(name = "update-parent")
class UpdateParentMojo extends AbstractMojo {
    @Parameter(defaultValue = '${project}', readonly = true, required = true)
    MavenProject mavenProject
    @Parameter( defaultValue = '${localRepository}', readonly = true, required = true)
    ArtifactRepository localRepository
    @Parameter( defaultValue = '${project.remoteArtifactRepositories}', readonly = true )
    protected List remoteArtifactRepositories
    @Component
    ArtifactMetadataSource artifactMetadataSource
    @Component
    ArtifactFactory artifactFactory

    void execute() throws MojoExecutionException, MojoFailureException {
        def artifact = artifactFactory.createParentArtifact(mavenProject.parent.groupId, mavenProject.parent.artifactId, mavenProject.parent.version)
        def versions = artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories)

        println versions
    }
}