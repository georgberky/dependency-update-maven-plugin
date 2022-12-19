package io.github.georgberky.maven.plugins.depsupdate

import com.jcraft.jsch.JSch.setConfig
import com.jcraft.jsch.Session
import org.apache.maven.settings.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import java.nio.file.Path

class JGitProvider(localRepositoryPath: Path, val settings: Settings, val connection: String) : GitProvider {
    val git: Git = Git(
        RepositoryBuilder()
            .readEnvironment()
            .findGitDir(localRepositoryPath.toFile())
            .setMustExist(true)
            .build()
    )

    private val initialBranch: String = git.repository.branch

    override fun hasRemoteBranch(remoteBranchName: String): Boolean {
        return git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
            .any { it.name == "refs/remotes/origin/$remoteBranchName" }
    }

    override fun hasRemoteBranchWithPrefix(remoteBranchNamePrefix: String): Boolean {
        return git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
            .any { it.name.startsWith("refs/remotes/origin/$remoteBranchNamePrefix") }
    }

    override fun checkoutNewBranch(branchName: String) {
        git.checkout()
            .setStartPoint(git.repository.fullBranch)
            .setCreateBranch(true)
            .setName(branchName)
            .call()
    }

    override fun add(filePattern: String) {
        git.add()
            .addFilepattern(filePattern)
            .call()
    }

    override fun commit(author: String, email: String, message: String) {
        git.commit()
            .setAuthor(PersonIdent(author, email))
            .setMessage(message)
            .call()
    }

    override fun push(localBranchName: String) {
        git.push()
            .setRefSpecs(RefSpec("$localBranchName:$localBranchName"))
            .apply {
                val uri = URIish(connection.removePrefix("scm:git:"))

                when (uri.scheme) {
                    "https" -> {
                        val (username, password) = usernamePassword(uri)

                        setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
                    }

                    else -> {
                        setTransportConfigCallback(
                            TransportConfigCallback { transport ->
                                if (transport !is SshTransport) return@TransportConfigCallback
                                val (_, password) = usernamePassword(uri)
                                if (password != null) {
                                    transport.sshSessionFactory = object : JschConfigSessionFactory() {
                                        override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
                                            session?.setPassword(password)
                                            session?.setConfig("StrictHostKeyChecking", "no")
                                        }
                                    }
                                } else {
                                    transport.sshSessionFactory = object : JschConfigSessionFactory() {
                                        override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
                                        }

                                        override fun createDefaultJSch(fs: FS?) =
                                            settings.getServer(uri.host).run {
                                                super.createDefaultJSch(fs).apply {
                                                    addIdentity(privateKey, passphrase)
                                                    setConfig("StrictHostKeyChecking", "no")
                                                }
                                            }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            // .setPushOptions(listOf("merge_request.create"))
            .call()
    }

    override fun checkoutInitialBranch() {
        git
            .checkout()
            .setStartPoint(git.repository.fullBranch)
            .setName(initialBranch)
            .call()
    }

    override fun close() {
        git.close()
    }

    private fun usernamePassword(uri: URIish) =
        if (uri.user != null && uri.pass != null) {
            uri.user to uri.pass
        } else {
            settings.getServer(uri.host)
                .run { username to password }
        }
}
