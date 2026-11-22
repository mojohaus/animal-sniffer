# Animal Sniffer

<!--
The MIT License

Copyright (c) 2009 codehaus.org.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
-->

# Animal Sniffer

Animal Sniffer provides tools to assist verifying that classes compiled with a newer JDK/API are compatible with an
older JDK/API.

Please note that for checking against a JDK version, the `--release` flag from Javac achieves the same, see
<https://openjdk.java.net/jeps/247>, and the plugin is in maintenance level for that use. The flag only knows JDK
APIs: Animal Sniffer remains the tool for checking against other APIs, such as Android API levels, or against a custom
signature.

## Introduction

What happens if you compile a program written with a version of an API that is different from the version you are
targetting? Well the answer depends on a number of things.

* Firstly we will assume that the API has an evolution contract, i.e., if a class or method or field
  is present in version v1, it will also be present in version v2 providing v1 is less than or equal to v2. A good
  example of this is the Java Runtime Library (a.k.a the JRE)
* If you are compiling with a newer version of the API, you should not get any compiler errors.
* If you are compiling with an older version of the API, and you have only used those features in the older version
  of the API, you should not get any compiler errors.
* If you are compiling with an older version of the API, and you have used some features only available in the newer
  version, you will get a compiler error.

If you are developing a project which must support running on JDK version 1.4, but your development system does not
have a JDK version 1.4 available, it can be quite easy to accidentally use methods or classes that are only available
in the newer version of the JDK. For example, a Maven plugin that must run on an older Java version, developed
on a machine with a newer JDK.

Animal sniffer can check the classes and method signatures that your compiled code uses and verify that you have
use only those classes and signatures available in the API you are targetting.

## What is checked

The checker reports every class, method and field that your classes reference and that the signature does not
contain. Since 1.29 it also checks the types of the declared method parameters, so a method that accepts a type
missing from the target API is reported even when its body never uses the type. Since 1.29.1, calls to
signature-polymorphic methods, such as `MethodHandle.invoke` and the `VarHandle` access modes, are matched by method
name on the owner and its super types, so they pass the check. `@IgnoreJRERequirement` and the other configured
annotations suppress the reports for what they annotate.

## Why the name?

Sun gives each of the JDK versions codenames. Most of these codenames are animal names. Animal-sniffer was concieved
as a utility to see what "animal" your code can run on.

## What does animal-sniffer provide?

The following tools are provided by animal sniffer:

* [A command line tool to dump the class file version number](./animal-sniffer/index.html). This helps you track
  down the offending jar file when you see `UnsupportedClassVersionError`.
* [A set of ANT tasks](./animal-sniffer-ant-tasks/index.html) for verifying that your classes comply with an API
  signature as well as tasks for creating API signatures from a JDK, or a collection or jar and class files, or a
  collection of other API signature files, or combination of these elements.
* [A rule for use in the maven-enforcer-plugin](./animal-sniffer-enforcer-rule/index.html) for verifying that your
  classes comply with an API signature.
* [A maven plugin](../animal-sniffer-maven-plugin/index.html) for verifying that your classes comply with an API
  signature as well as for creating API signatures from a JDK, or the current module's classes, or the current
  module's dependencies, or a collection of other API signature files, or combination of these elements.

