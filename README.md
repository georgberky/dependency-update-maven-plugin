# Dependency Update Maven Plugin

[![Build Status](https://cloud.drone.io/api/badges/georgberky/dependency-update-maven-plugin/status.svg)](https://cloud.drone.io/georgberky/dependency-update-maven-plugin) [![codecov](https://codecov.io/gh/georgberky/dependency-update-maven-plugin/branch/master/graph/badge.svg?token=XMHYGY0L3A)](https://codecov.io/gh/georgberky/dependency-update-maven-plugin)

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
