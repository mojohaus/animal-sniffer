<?xml version="1.0" encoding="UTF-8"?>
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
<faqs id="FAQ" title="Frequently Asked Questions">
  <part id="general">
    <title>General</title>

    <faq id="purpose">
      <question>What does the Animal Sniffer Maven Plugin do?</question>
      <answer>
        <p>The Animal Sniffer Maven Plugin provides a means to verify that your classes only use the classes, methods and fields provided by specific API signatures that you specify.  The plugin also provides a means to generate API signatures from your project and/or a specific JDK.</p>
      </answer>
    </faq>

    <faq id="name">
      <question>Why the name?
      </question>
      <answer>
        <p>
          The plugin's original author (Kohsuke Kawaguchi) named the plugin
          animal-sniffer because it was originally designed to detect which
          version of a JRE had been used to compile your classes.  As such it was
          detecting the "scent" of each JRE.
        </p>
        <p>
          Sun has traditionally named each of the major versions of its JRE after
          different animals.  For example 1.5 (a.k.a Java SE 5.0) was known as
          Tiger; 1.6 (a.k.a. Java SE 6.0) was known as Mustang.  Since this plugin
          detects the "scent" of each of these "animals" it got the name
          "animal-sniffer".
        </p>
      </answer>
    </faq>
    <faq id="java.net">
      <question>Which is the "official" animal-sniffer host: Codehaus or java.net?</question>
      <answer>
        <p>
          This project is the "official" project.  The
          <a href="https://animal-sniffer.dev.java.net">java.net</a> project
          was started by Kohsuke Kawaguchi before he was granted commit access
          to the mojo project. Since Kohsuke was granted commit access in part
          to allow the migration of animal-sniffer to Codehaus, he has decreed
          the Codehaus version the "official" version as well as re-licensed
          the Codehaus version under the MIT License. The java.net version
          remains licensed under CDDL.
        </p>
        <p>
          This version (by virtue of belonging to mojo.codehaus.org) has the
          advantage of being available in the
          <a href="http://repo1.maven.org">Maven Central repository</a>
          which means that projects which publish to the Maven Central
          repository can use this version in their <code>pom.xml</code>.
          <strong>Note:</strong> Projects which publish to the Maven Central repository must
          only reference dependencies and plugins available from the
          Maven Central repository and may not reference other repositories.
        </p>
        <p>
          In addition, this version has been subject to on-going development
          effort.
        </p>
      </answer>
    </faq>
  </part>
</faqs>
