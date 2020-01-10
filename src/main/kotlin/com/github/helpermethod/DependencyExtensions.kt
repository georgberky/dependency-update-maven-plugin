package com.github.helpermethod

import org.apache.maven.model.Dependency

fun Dependency.isConcrete(): Boolean {
    return version != null && !version.contains("\\$\\{[^}]+}/".toRegex())
}