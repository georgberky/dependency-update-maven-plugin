# Dependency Update Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/com.github.helpermethod/dependency-update-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.helpermethod%22%20AND%20a:%22dependency-update-maven-plugin%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://raw.githubusercontent.com/helpermethod/dependency-update-maven-plugin/master/LICENSE)

The `Dependency Update Maven Plugin` analyzes the dependencies of your Maven project. If a more recent version for a dependency is found, the plugin will

* create a new branch
* update the dependency version in the POM
* commit and push the change
* create a merge request

## Features

* supports Public Key, Username Password and Token-Based Authentication

## Quickstart

```sh
mvn com.github.helpermethod:dependency-update-maven-plugin:0.4.0:update
```
