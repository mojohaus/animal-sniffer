# Animal Sniffer Enforcer Rule

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

# Animal Sniffer Enforcer Rule

The Animal Sniffer Enforcer Rule is used to check your classes against previously generated signatures.
This enforcer rule is called animal sniffer because the principal signatures that are used are those of
the Java Runtime, and since Sun traditionally names the different versions of its Java Runtimes after
different animals, the original project from which this enforcer rule was developed was called "Animal Sniffer".

## Rules Overview

The Animal Sniffer Enforcer Rule has the following rule.

* `checkSignatureRule` checks a project against an API signature.

## Usage

General instructions on how to use the Animal Sniffer Enforcer Rule can be found on the [usage page](./usage.html).
Some more specific use cases are described in the examples given below. Last but not least, users occasionally
contribute additional examples, tips or errata to the
[enforcer rule's wiki page](http://docs.codehaus.org/display/MAVENUSER/Animal+Sniffer+Enforcer+Rule).

In case you still have questions regarding the enforcer rule's usage, feel free to contact the
[user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could
already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching
the [mail archive](./mailing-lists.html).

If you feel like the enforcer rule is missing a feature or has a defect, you can fill a feature request or bug report
in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description
of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this
reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much
appreciated. Of course, patches are welcome, too. Contributors can check out the project from our
[source repository](./scm.html) and will find supplementary information in the
[guide to helping with Maven](http://maven.apache.org/guides/development/guide-helping.html).

## Examples

To provide you with better understanding of some usages of Animal Sniffer Enforcer Rule,
you can take a look into the following examples:

* [Checking a project against API signatures](./examples/checking-signatures.html)

