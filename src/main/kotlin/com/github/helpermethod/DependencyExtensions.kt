package com.github.helpermethod

import org.apache.maven.model.Dependency

fun Dependency.isConcrete() =
    version != null && !version.contains("\\$\\{[^}]+}".toRegex())