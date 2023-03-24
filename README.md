# MojoHaus AnimalSniffer Maven Plugin

This is the [animal-sniffer-maven-plugin](https://www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/).

Please note this plugin is now in maintenance level as the exact same feature can be now easily achieved using the `--release` 
flag from Javac see (https://openjdk.java.net/jeps/247)

[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/animal-sniffer-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.mojo/animal-sniffer-maven-plugin)
[![Build Status](https://github.com/mojohaus/animal-sniffer/actions/workflows/maven.yml/badge.svg)](https://github.com/mojohaus/animal-sniffer/actions/workflows/maven.yml)

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
