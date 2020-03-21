package com.github.helpermethod

import com.jcraft.jsch.Session
import org.apache.maven.settings.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.util.FS

// TODO: interaction test
class JGitProvider(val git: Git, val connection: String, val settings: Settings) : GitProvider {
    override fun hasRemoteBranch(branchName: String): Boolean =
            git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().none { it.name == "refs/remotes/origin/$branchName" }

    override fun checkout(branchName: String) {
        git
                .checkout()
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

    override fun commit(author: String, message: String) {
        git.commit()
                .setAuthor(PersonIdent(author, ""))
                .setMessage(message)
                .call()
    }

    override fun push(branchName: String) {
        git
                .push()
                .setRefSpecs(RefSpec("$branchName:$branchName"))
                .apply {
                    val uri = URIish(connection.removePrefix("scm:git:"))

                    when (uri.scheme) {
                        "https" -> {
                            val (username, password) = usernamePassword(uri)

                            setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
                        }
                        else -> {
                            setTransportConfigCallback(TransportConfigCallback { transport ->
                                if (transport !is SshTransport) return@TransportConfigCallback

                                transport.sshSessionFactory = object : JschConfigSessionFactory() {
                                    override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
                                    }

                                    override fun createDefaultJSch(fs: FS?) =
                                            settings.getServer(uri.host).run {
                                                super.createDefaultJSch(fs).apply { addIdentity(privateKey, passphrase) }
                                            }
                                }
                            })
                        }
                    }
                }
                .setPushOptions(listOf("merge_request.create"))
                .call()
    }

    override fun close() {
        git.close()
    }

    private fun usernamePassword(uri: URIish) =
            if (uri.user != null) uri.user to uri.pass else settings.getServer(uri.host).run { username to password }
}