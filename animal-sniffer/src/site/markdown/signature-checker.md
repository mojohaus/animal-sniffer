 ~~ The MIT License
 ~~
 ~~ Copyright (c) 2008 Kohsuke Kawaguchi and codehaus.org.
 ~~
 ~~ Permission is hereby granted, free of charge, to any person obtaining a copy
 ~~ of this software and associated documentation files (the "Software"), to deal
 ~~ in the Software without restriction, including without limitation the rights
 ~~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 ~~ copies of the Software, and to permit persons to whom the Software is
 ~~ furnished to do so, subject to the following conditions:
 ~~
 ~~ The above copyright notice and this permission notice shall be included in
 ~~ all copies or substantial portions of the Software.
 ~~
 ~~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ~~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ~~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ~~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ~~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ~~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 ~~ THE SOFTWARE.
 ~~
 
 -----
 Signature Checker
 -----
 Kohsuke Kawaguchi
 -----
 2008-11-15
 -----


What is this?

  It's common for a Java project to compile with later versions of JDK than it minimally requires. For example, when Hudson runs on Java6 it takes advantages of those features, but it can also run on Java5 without those advanced features.

  The technique to do this is well understood. Here's one such code fragment taken from Hudson:

-----------------
try {
    for (ThreadInfo ti : Functions.getThreadInfos())
        r.put(ti.getThreadName(),Functions.dumpThreadInfo(ti));
} catch (LinkageError _) {
    // not in JDK6. fall back to JDK5
    ...
}
-----------------

  This is desirable, since you can take advantages of the latest JavaSE features without forcing users to upgrade.

  The problem is that you now need to compile with JDK6, so when other parts of code accidentally depends on new additions in Java6 and breaks the minimum Java5 requirement, your build process won't complain.

  If you are lucky, your tests will catch it, but no test attain 100% of code coverage, so there's still a good chance that such a problem slips into the production code. (For example, I've been bitten a few times by using <<<IOException(String,Throwable)>>> constructor that was added in 1.6, since I typically use it only in handling errors that tend not to be tested well.)


Animal sniffer to the rescue

  The idea of the tool is simple. I first run a "signature builder" with various versions of JREs, capturing all the method and field signatures from them. Those are then uploaded into a Maven repository to be downloaded later by your Maven.

  I then wrote a separate tool called "signature checker", which uses this signature file and inspect your classes. If your classes depend on things that don't exist in the signature list, you get an error message. This tool is packaged up as a Maven plugin, so to use this, you just add the following snippet inside your \<build> element in <<<pom.xml>>>:

-----------------
<plugin>
  <!-- make sure our code doesn't have 1.6 dependencies except where we know it -->
  <groupId>org.jvnet</groupId>
  <artifactId>animal-sniffer</artifactId>
  <version>1.2</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <signature>
          <groupId>org.jvnet.animal-sniffer</groupId>
          <artifactId>java1.5</artifactId>
          <version>1.0</version>
        </signature>
      </configuration>
    </execution>
  </executions>
</plugin>
-----------------

  The nested \<signature> element specifies the signature list to use. In addition to java1.5, {{{http://maven.dyndns.org/2/org/jvnet/animal-sniffer/}java1.3, java1.4, and java1.6 available}}.


Running this less often

  If you don't want to do this for every Maven build, you can instead have the following snippet:

-----------------
<plugin>
  <!-- make sure our code doesn't have 1.6 dependencies except where we know it -->
  <groupId>org.jvnet</groupId>
  <artifactId>animal-sniffer</artifactId>
  <version>1.2</version>
  <configuration>
    <signature>
      <groupId>org.jvnet.animal-sniffer</groupId>
      <artifactId>java1.5</artifactId>
      <version>1.0</version>
    </signature>
  </configuration>
</plugin>
-----------------

  And then you can run <<<mvn compile animal-sniffer:check>>> to check the dependency, or you can further add the following POM snippet so that the check is performed automatically during a release:

-----------------
<plugin>
  <artifactId>maven-release-plugin</artifactId>
  <configuration>
    <goals>install animal-sniffer:check deploy</goals>
  </configuration>
</plugin>
-----------------

  The tool uses ASM and statically analyze the code, so it doesn't miss any reference, unlike test based approach.

Marking dependencies explicitly

  Now, in the places where you knowingly use features that go beyond the minimum requirement, you put the <<<@IgnoreJRERequirement>>> annotation on a method. This is a signal from you to the checker that you're aware of the dependency there and you know what you are doing.

  For this code to compile, you need to add <<<animal-sniffer.jar>>> to the dependency list. This annotation is configured as <<<@Retention(CLASS)>>>, so you don't need this jar to be at runtime. To tell Maven not to put it in the runtime, this fragment includes <<<\<optional>true\</optional> >>>:


-----------------
<dependency>
  <!-- for JRE requirement check annotation -->
  <groupId>org.jvnet</groupId>
  <artifactId>animal-sniffer</artifactId>
  <version>1.2</version>
  <optional>true</optional>
</dependency>
-----------------

  There are certain edge cases that this tool doesn't handle correctly (like the case when a visibility of a method changes from 'protected' to 'public' between Java5 to Java6 --- not that I know such a case exists), but I think it runs pretty well, at least on Hudson, and the added peace of mind is priceless.

  As usual, I'm always looking for more people to work on any of my projects, so if you are interested, please send me an e-mail, and you'll be a committer right away.

