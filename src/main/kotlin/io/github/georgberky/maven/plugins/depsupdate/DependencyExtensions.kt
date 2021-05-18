package io.github.georgberky.maven.plugins.depsupdate

import org.apache.maven.model.Dependency

fun Dependency.isConcrete() =
    version != null && !version.contains("\\$\\{[^}]+}".toRegex())