package com.github.helpermethod

import org.apache.maven.artifact.Artifact
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
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent

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
        // update parent version
        Artifact parent = artifactFactory.createParentArtifact(mavenProject.parent.groupId, mavenProject.parent.artifactId, mavenProject.parent.version)
        def versions = artifactMetadataSource.retrieveAvailableVersions(parent, localRepository, remoteArtifactRepositories)
        def latestParentVersion = versions.max().toString()

        // no changes
        if (parent.version != latestParentVersion) return

        // git checkout -b
        def git = Git.open(mavenProject.basedir)
        git.checkout().tap {
            createBranch = true
            name = "continuous-dependency-update/$parent"
        }.call()

        def modifiedPom = new XmlParser(false, false).parse(mavenProject.file).tap {
            children().find { it.name() == 'parent' }.version[0].value = latestParentVersion
        }

        // rewrite POM
        mavenProject.file.withPrintWriter {
            new XmlNodePrinter(it, " " * 4).tap {
                preserveWhitespace = true
            }.print(modifiedPom)
        }

        // git add
        git.add().addFilepattern('pom.xml').call()

        // git commit
        git.commit().tap {
            author = new PersonIdent('continuous-dependency-update', null)
            message = "Bump ${parent.groupId}:${parent.artifactId} from $parent.version to $latestParentVersion"
        }.call()
    }
}