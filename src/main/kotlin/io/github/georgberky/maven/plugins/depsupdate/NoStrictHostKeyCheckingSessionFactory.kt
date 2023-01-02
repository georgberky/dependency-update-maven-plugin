package io.github.georgberky.maven.plugins.depsupdate

import com.jcraft.jsch.Session
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.JschSession
import org.eclipse.jgit.transport.OpenSshConfig

class NoStrictHostKeyCheckingSessionFactory(private val password: String) : JschConfigSessionFactory() {
    override fun configure(host: OpenSshConfig.Host?, session: Session?) {
        session?.setPassword(password)
        session?.setConfig("StrictHostKeyChecking", "no")
    }
}
