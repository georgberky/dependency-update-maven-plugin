package com.github.helpermethod


import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

import static groovy.io.FileType.FILES
import static org.twdata.maven.mojoexecutor.MojoExecutor.*

@Mojo(name = "update-dependencies")
class UpdateDependenciesMojo extends AbstractMojo {
    @Parameter(defaultValue = '${project}', readonly = true, required = true)
    MavenProject mavenProject
    @Parameter(defaultValue = '${session}', readonly = true, required = true)
    MavenSession mavenSession
    @Parameter(defaultValue = '${project.build.directory}', readonly = true, required = true)
    File buildDirectory
    @Parameter(readonly = true, required = true)
    URI gitlabUrl;
    @Parameter(readonly = true, required = true)
    String gitlabUsername;
    @Parameter(readonly = true, required = true)
    String gitlabPassword;
    @Component
    BuildPluginManager pluginManager

    void execute() throws MojoExecutionException, MojoFailureException {
        executeMojo(
            plugin(
                groupId("org.codehaus.mojo"),
                artifactId("versions-maven-plugin"),
                version("2.7")
            ),
            goal("dependency-updates-report"),
            configuration(
                element("formats", "xml"),
                element("processDependencyManagementTransitive", "false")
            ),
            executionEnvironment(
                mavenProject,
                mavenSession,
                pluginManager
            )
        )

        def reports = [] as List<File>

        buildDirectory.traverse(type: FILES, nameFilter: ~/dependency-updates-report.xml/) {
            reports << it
        }

        reports
            .collect { new XmlParser().parse(it) }
            .collectMany { it.dependencies.dependency }
            .collect { [groupId: it.groupId.text(), artifactId: it.artifactId.text(), nextVersion: it.nextVersion.text()]}
            .each { println(it) }
    }
}