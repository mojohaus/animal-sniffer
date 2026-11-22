# MojoHaus Animal Sniffer

[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/animal-sniffer-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.mojo/animal-sniffer-maven-plugin)
[![Build Status](https://github.com/mojohaus/animal-sniffer/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/mojohaus/animal-sniffer/actions/workflows/maven.yml?query=branch%3Amaster)

Animal Sniffer checks that classes compiled against a newer JDK or API only use classes, methods
and fields that exist in an older one. This repository holds the signature builder and checker, the
[Maven plugin](https://www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/), the Maven
Enforcer rule, the Ant tasks and the `@IgnoreJRERequirement` annotation.

The plugin is in maintenance mode for checking against a JDK version, which the `--release` flag of
`javac` does ([JEP 247](https://openjdk.org/jeps/247)). Animal Sniffer remains the tool for other
APIs, such as Android API levels, and for custom signatures.

The plugin requires Maven 3.9.0 or later and Java 8 or later. The
[project site](https://www.mojohaus.org/animal-sniffer/) has usage instructions for each module.

Contributor and agent notes are in [AGENTS.md](AGENTS.md).

## Releasing

* Make sure `gpg-agent` is running.
* Make sure all tests pass `mvn clean verify -Prun-its`
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site 
mvn scm-publish:publish-scm
```

