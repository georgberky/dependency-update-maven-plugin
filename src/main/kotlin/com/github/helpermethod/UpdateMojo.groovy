package com.github.helpermethod

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import groovy.transform.CompileStatic
import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.factory.ArtifactFactory
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.model.Dependency
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
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.util.FS
import org.jdom2.JDOMFactory
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.SAXHandler
import org.jdom2.input.sax.SAXHandlerFactory
import org.jdom2.output.XMLOutputter
import org.jdom2.xpath.XPathFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXException

import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE
import static org.eclipse.jgit.transport.OpenSshConfig.Host
import static org.jdom2.filter.Filters.element

@CompileStatic
@Mojo(name = "update")
class UpdateMojo extends AbstractMojo {
    @Parameter(defaultValue = '${project}', readonly = true)
    MavenProject mavenProject
    @Parameter(defaultValue = '${localRepository}', readonly = true)
    ArtifactRepository localRepository
    @Parameter(defaultValue = '${project.remoteArtifactRepositories}', readonly = true)
    List remoteArtifactRepositories
    @Parameter(defaultValue = '${settings}', readonly = true)
    Settings settings

    @Parameter(property = 'connectionUrl', defaultValue = '${project.scm.connection}')
    @Parameter(property = 'developerConnectionUrl', defaultValue = '${project.scm.developerConnection}')
    String developerConnectionUrl
    @Parameter(property = "connectionType", defaultValue = 'connection')
    String connectionType

    @Component
    ArtifactFactory artifactFactory
    @Component
    ArtifactMetadataSource artifactMetadataSource

    void execute() {
        withGit { git, head ->
            [this.&parentUpdate, this.&dependencyManagementUpdates, this.&dependencyUpdates]
                .collectMany { it() }
                .each { update ->
                    def branchName = "dependency-update/${update.groupId}-${update.artifactId}-${update.latestVersion}"

                    if (git.branchList().setListMode(REMOTE).call().find { it.name == "refs/remotes/origin/$branchName" }) {
                        log.info("Merge request for $branchName already exists.")

                        return
                    }

                    git
                        .checkout()
                        .setStartPoint(head)
                        .setCreateBranch(true)
                        .setName(branchName)
                        .call()

                    withPom(update.modification)

                    git
                        .add()
                        .addFilepattern('pom.xml')
                        .call()

                    git
                        .commit()
                        .setAuthor(new PersonIdent('dependency-update-bot', ''))
                        .setMessage("Bump $update.artifactId from $update.currentVersion to $update.latestVersion")
                        .call()

                    git
                        .push()
                        .setRefSpecs(new RefSpec("$branchName:$branchName"))
                        .tap {
                            def connectionUri = new URIish(connection - 'scm:git:')

                            switch (connectionUri.scheme) {
                                case 'https':
                                    setCredentialsProvider(createUsernamePasswordCredentialsProvider(connectionUri))

                                    break
                                default:
                                    setTransportConfigCallback(new TransportConfigCallback() {
                                        void configure(Transport transport) {
                                            ((SshTransport) transport).sshSessionFactory = new JschConfigSessionFactory() {
                                                @Override
                                                protected JSch createDefaultJSch(FS fs) throws JSchException {
                                                    settings.getServer(connectionUri.host).with {
                                                        super.createDefaultJSch(fs).tap { addIdentity(privateKey, passphrase) }
                                                    }
                                                }

                                                protected void configure(Host host, Session session) {
                                                }
                                            }
                                        }
                                    })
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
        def namespaceIgnoringSaxBuilder = new SAXBuilder(SAXHandlerFactory: new SAXHandlerFactory() {
            @Override
            SAXHandler createSAXHandler(JDOMFactory factory) {
                return new SAXHandler() {
                    @Override
                    void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
                        super.startElement('', localName, qName, atts);
                    }

                    @Override
                    void startPrefixMapping(String prefix, String uri) throws SAXException {
                    }
                }
            }
        })
        def pom = namespaceIgnoringSaxBuilder.build(mavenProject.file)

        cl(pom)

        new XMLOutputter().output(pom, mavenProject.file.newWriter())
    }

    private String getConnection() {
        connectionType == 'developerConnection' ? developerConnectionUrl : connectionUrl
    }

    private def createUsernamePasswordCredentialsProvider(uri) {
        uri.user ? new UsernamePasswordCredentialsProvider(uri.user, uri.pass) : settings.getServer(uri.host).with { new UsernamePasswordCredentialsProvider(it.username, it.password) }
    }

    private def parentUpdate() {
        ([mavenProject.parentArtifact] - null)
            .findResults { artifact ->
                update(artifact) { version, pom ->
                    XPathFactory.instance().compile("/project/parent/version", element()).evaluateFirst(pom)?.text = version
                }
            }
    }

    private def dependencyManagementUpdates() {
        (mavenProject.originalModel.dependencyManagement?.dependencies ?: [])
            .findAll(this.&isConcrete)
            .collect(this.&createDependencyArtifact)
            .findResults { artifact ->
                update(artifact) { version, pom ->
                    XPathFactory.instance().compile("/project/dependencyManagement/dependencies/dependency[groupId = '$artifact.groupId' && artifactId = '$artifact.artifactId' && version = '$artifact.version']/version", element()).evaluateFirst(pom)?.text = version
                }
            }
    }

    private def dependencyUpdates() {
        mavenProject.originalModel.dependencies
            .findAll(this.&isConcrete)
            .collect(this.&createDependencyArtifact)
            .findResults { artifact ->
                update(artifact) { version, pom ->
                    XPathFactory.instance().compile("/project/dependencies/dependency[groupId = '$artifact.groupId' && artifactId = '$artifact.artifactId' && version = '$artifact.version']/version", element()).evaluateFirst(pom)?.text = version
                }
            }
    }

    private static def isConcrete(Dependency dependency) {
        dependency.version && !(dependency.version =~ /\$\{[^}]+}/)
    }

    private def createDependencyArtifact(Dependency dependency) {
        artifactFactory.createDependencyArtifact(dependency.groupId, dependency.artifactId, createFromVersionSpec(dependency.version), dependency.type, dependency.classifier, dependency.scope, dependency.optional)
    }

    private def update(Artifact artifact, Closure modification) {
        def latestVersion = retrieveLatestVersion(artifact)

        if (artifact.version == latestVersion) return null

        new Update(artifact.groupId, artifact.artifactId, artifact.version, latestVersion, modification.curry(latestVersion))
    }

    private def getLatestVersion(Artifact artifact) {
        artifactMetadataSource
            .retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories)
            .findAll { it.qualifier != 'SNAPSHOT' }
            .max()
            .toString()
    }
}