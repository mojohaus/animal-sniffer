# MojoHaus AnimalSniffer Maven Plugin

This is the [animal-sniffer-maven-plugin](http://www.mojohaus.org/animal-sniffer/animal-sniffer-maven-plugin/).

Please note this plugin is now in maintenance level as the exact same feature can be now easily achieved using the `--release` 
flag from Javac see (http://openjdk.java.net/jeps/247)

[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/animal-sniffer-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.mojo/animal-sniffer-maven-plugin)
[![Build Status](https://travis-ci.org/mojohaus/animal-sniffer.svg?branch=master)](https://travis-ci.org/mojohaus/animal-sniffer)

## Releasing

* Make sure `gpg-agent` is running.
* Make sure all tests pass `mvn clean verify -Prun-its`
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
