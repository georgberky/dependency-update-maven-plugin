package com.github.helpermethod

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

@CompileStatic
@Mojo(name = "update-dependencies")
class UpdateDependenciesMojo extends AbstractMojo {
    @Parameter(defaultValue = '${project}', readonly = true)
    MavenProject mavenProject

    @Parameter(defaultValue = '${localRepository}', readonly = true)
    ArtifactRepository localRepository
    @Parameter(defaultValue = '${project.remoteArtifactRepositories}', readonly = true)
    List remoteArtifactRepositories

    @Parameter(property = 'connectionUrl', defaultValue = '${project.scm.connection}')
    String connectionUrl
    @Parameter(property = 'developerConnectionUrl', defaultValue = '${project.scm.developerConnection}')
    String developerConnectionUrl
    @Parameter(property = "connectionType", defaultValue = "connection")
    String connectionType

    @Parameter(defaultValue = '${settings}', readonly = true)
    Settings settings

    @Component
    ArtifactMetadataSource artifactMetadataSource
    @Component
    ArtifactFactory artifactFactory

    void execute() {
        // update parent version
        def parent = artifactFactory.createParentArtifact(mavenProject.parent.groupId, mavenProject.parent.artifactId, mavenProject.parent.version)
        def versions = artifactMetadataSource.retrieveAvailableVersions(parent, localRepository, remoteArtifactRepositories)
        def latestParentVersion = versions.max().toString()

        // no changes
        if (parent.version == latestParentVersion) return

        def git = new Git(
            new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(mavenProject.basedir)
                .setMustExist(true)
                .build()
        )

        // git checkout -b
        git.checkout()
            .setCreateBranch(true)
            .setName("continuous-dependency-update/${parent.groupId}-${parent.artifactId}-${latestParentVersion}")
            .call()

        def modifiedPom = updatePom(latestParentVersion)

        // rewrite POM
        mavenProject.file.withPrintWriter {
            new XmlNodePrinter(it, " " * 4).tap {
                preserveWhitespace = true
            }.print(modifiedPom)
        }

        // git add
        git.add().addFilepattern('pom.xml').call()

        // git commit
        git.commit()
            .setAuthor(new PersonIdent('continuous-update', ''))
            .setMessage("Bump ${parent.artifactId} from ${parent.version} to $latestParentVersion")
            .call()

        // git push
        git.push()
            .setRefSpecs([new RefSpec("+/refs/head/continuous-dependency-update/${parent.groupId}-${parent.artifactId}-${latestParentVersion}:refs/remotes/origin/${parent.groupId}-${parent.artifactId}-${latestParentVersion}")])
            .tap {
                def connectionUri = (connection - 'scm:git').toURI()

                switch (connectionUri.scheme) {
                    case ~/https?/:
                        def credentials = getCredentials(connectionUri)

                        setCredentialsProvider(new UsernamePasswordCredentialsProvider(credentials.username, credentials.password))

                        break
                    case 'ssh':
                        setTransportConfigCallback { Transport transport ->
                            (transport as SshTransport).sshSessionFactory = { host, session -> } as JschConfigSessionFactory
                        }

                        break
                    // TODO handle default
                }
            }
            .setPushOptions(['merge_request.create'])
            .call()
    }

    @CompileDynamic
    private def updatePom(latestParentVersion) {
        new XmlParser(false, false).parse(mavenProject.file).tap {
            // TODO replace with list of callbacks
            children().find { it.name() == 'parent' }.version[0].value = latestParentVersion
        }
    }

    private String getConnection() {
        connectionType == 'developerConnection' ? developerConnectionUrl : connectionUrl
    }

    private Credentials getCredentials(URI connectionUri) {
        if (connectionUri.userInfo) {
            return connectionUri.userInfo.split(":").with {
                new Credentials(it[0], it[1])
            }
        }

        settings.getServer(connectionUri.host).with {
            new Credentials(username, password)
        }
    }

    @Immutable
    private static class Credentials {
        String username
        String password
    }
}