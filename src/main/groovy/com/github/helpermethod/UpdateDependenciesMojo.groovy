package com.github.helpermethod

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS

import static org.eclipse.jgit.transport.OpenSshConfig.Host

@Mojo(name = "update-dependencies")
class UpdateDependenciesMojo extends AbstractMojo {
    @Parameter(defaultValue = '${project}', readonly = true)
    MavenProject mavenProject
    @Parameter(defaultValue = '${localRepository}', readonly = true)
    ArtifactRepository localRepository
    @Parameter(defaultValue = '${project.remoteArtifactRepositories}', readonly = true)
    List remoteArtifactRepositories
    @Parameter(defaultValue = '${settings}', readonly = true)
    Settings settings

    @Parameter(property = 'connectionUrl', defaultValue = '${project.scm.connection}')
    String connectionUrl
    @Parameter(property = 'developerConnectionUrl', defaultValue = '${project.scm.developerConnection}')
    String developerConnectionUrl
    @Parameter(property = "connectionType", defaultValue = 'connection')
    String connectionType
    @Parameter(property = 'allowSnapshots', defaultValue = 'false')
    boolean allowSnapshots

    @Component
    ArtifactMetadataSource artifactMetadataSource

    void execute() {
        withGit { git, head ->
            [this.&parentUpdate, this.&dependencyUpdates]
                .findResults { it() }
                .flatten()
                .each { update ->
                    git
                        .checkout()
                        .setStartPoint(head)
                        .setCreateBranch(true)
                        .setName("continuous-dependency-update/${update.groupId}-${update.artifactId}-${update.latestVersion}")
                        .call()

                    withPom(update.modification)

                    git
                        .add()
                        .addFilepattern('pom.xml')
                        .call()

                    git
                        .commit()
                        .setAuthor(new PersonIdent('continuous-dependency-update-bot', ''))
                        .setMessage("Bump ${update.artifactId} from ${update.currentVersion} to $update.latestVersion")
                        .call()

                    git
                        .push()
                        .setRefSpecs(new RefSpec("continuous-dependency-update/${update.groupId}-${update.artifactId}-${update.latestVersion}:continuous-dependency-update/${update.groupId}-${update.artifactId}-${update.latestVersion}"))
                        .tap {
                            def connectionUri = new URIish(connection - 'scm:git:')

                            switch (connectionUri.scheme) {
                                case 'https':
                                    def (username, password) = getUsernamePasswordCredentials(connectionUri)

                                    credentialsProvider = new UsernamePasswordCredentialsProvider(username, password)

                                    break
                                default:
                                    def (privatekey, passphrase) = getPublicKeyCredentials(connectionUri)

                                    transportConfigCallback = new TransportConfigCallback() {
                                        void configure(Transport transport) {
                                            transport.sshSessionFactory = new JschConfigSessionFactory() {
                                                @Override
                                                protected JSch createDefaultJSch(FS fs) throws JSchException {
                                                    super.createDefaultJSch(fs).tap {
                                                        addIdentity(privatekey, passphrase)
                                                    }
                                                }

                                                protected void configure(Host host, Session session) {
                                                    // NOOP
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                        .setPushOptions(['merge_request.create'])
                        .call()
                }
        }
    }

    def withGit(cl) {
        def git = new Git(
            new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(mavenProject.basedir)
                .setMustExist(true)
                .build()
        )

        cl(git, git.repository.fullBranch)

        git.close()
    }

    def withPom(cl) {
        def pom = new XmlParser(false, false).parse(mavenProject.file)

        cl(pom)

        mavenProject.file.withPrintWriter {
            new XmlNodePrinter(it, " " * 4).tap { preserveWhitespace = true }.print(pom)
        }
    }

    private String getConnection() {
        connectionType == 'developerConnection' ? developerConnectionUrl : connectionUrl
    }

    private def getUsernamePasswordCredentials(uri) {
        uri.user ? [uri.user, uri.pass] : settings.getServer(uri.host).with { [it.username, it.password] }
    }

    private def getPublicKeyCredentials(uri) {
        settings.getServer(uri.host).with { [it.privateKey, it.passphrase] }
    }

    private def parentUpdate() {
        if (!mavenProject.parentArtifact) return []

        update(mavenProject.parentArtifact) { version, pom ->
            pom.children().find { it.name() == 'parent' }.version[0].value = version
        }
    }

    private def dependencyUpdates() {
        mavenProject.dependencyArtifacts.findResults { artifact ->
            update(artifact) { version, pom ->
                pom.dependencies.dependency.find {
                    it.artifactId == artifact.artifactId && it.groupId == artifact.groupId && it.version == artifact.version
                }.version[0].value = version
            }
        }
    }

    private def update(artifact, modification) {
        def versions = artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories)
        def latestVersion = versions.findAll { it.qualifier != 'SNAPSHOT' }.max().toString()

        if (artifact.version == latestVersion) return null

        [
            groupId       : artifact.groupId,
            artifactId    : artifact.artifactId,
            currentVersion: artifact.version,
            latestVersion : latestVersion,
            modification  : modification.curry(latestVersion)
        ]
    }
}