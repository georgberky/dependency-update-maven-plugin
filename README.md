# Dependency Update Maven Plugin

[![MavenBuild](https://github.com/georgberky/dependency-update-maven-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/georgberky/dependency-update-maven-plugin/actions/workflows/build.yml) [![codecov](https://codecov.io/gh/georgberky/dependency-update-maven-plugin/branch/master/graph/badge.svg?token=XMHYGY0L3A)](https://codecov.io/gh/georgberky/dependency-update-maven-plugin)

The `Dependency Update Maven Plugin` analyzes the dependencies of your Maven project.

If a more recent version of a dependency is found, the plugin will

* create a new branch
* update the dependency version in the POM
* commit and push the change
* create a merge request

## Features

* supports Public Key, Username Password and Token-Based Authentication

## Quickstart

```sh
mvn io.github.georgberky.maven.plugins.depsupdate:dependency-update-maven-plugin:update
```

## Goals

### update

[Update Mojo](https://pages.github.com/georgberky/dependency-update-maven-plugin/)
