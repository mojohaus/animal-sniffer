 ~~ The MIT License
 ~~
 ~~ Copyright (c) 2009 codehaus.org.
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
 Generating signatures
 -----
 Stephen Connolly
 ------
 2009-11-05
 ------

Generating signatures

* Basic

  To generate signatures of any API, you simple construct a invoke the <<<build-signatures>>> task
  and provide the path(s) of all the classes / jar files which form the API. For example:
  
---
<project ... xmlns:as="antlib:org.codehaus.mojo.animal_sniffer">
  ...
  <target ...>
    ...
    <as:build-signatures destfile="dist/api.sig">
      <path>
        <fileset dir="classes"/>
        <fileset dir="lib" includes="log4j.jar"/>
      </path>
    </as:build-signatures>
    ...
  </target>
  ...
</project>
---  

* Generating "pure" signatures

  The above examples generate signatures of the every class in the provided path.
  There are some situations where you may not want to include all the classes on the 
  provided path, for example when generating the signature of the JRE, as
  that includes all the implementation classes which are not part of the
  public contract of the JRE.  For example sun.misc.BASE64Encoder is part
  of Sun's JRE runtime libraries, but is not part of the JRE specification
  and if you run on a JRE produced by a different vendor, it is highly likely
  that that class will not be available to your program.

  In order to ensure that the Java signatures you generate only include those
  classes which you want, you may need to tune your signatures.

** Inclusion based tuning

  One technique is to only include those classes which you know are part of the 
  JRE public specification.  For example:

---
<project ... xmlns:as="antlib:org.codehaus.mojo.animal_sniffer">
  ...
  <target ...>
    ...
    <as:build-signatures ...>
      ...
      <includeClasses className="java.*"/>
      <includeClasses className="javax.*"/>
      <includeClasses className="org.omg.*"/>
      <includeClasses className="org.w3c.dom.*"/>
      <!-- etc -->
      ...
    </as:build-signatures>
    ...
  </target>
  ...
</project>
---

  This requires that you known exactly what classes to include.

** Exclusion based tuning

  The other technique is to specify which classes are not to be included
  (Note that a combination of the two can also be used.) For example:

---
<project ... xmlns:as="antlib:org.codehaus.mojo.animal_sniffer">
  ...
  <target ...>
    ...
    <as:build-signatures ...>
      ...
      <excludeClasses className="com.sun.*"/>
      <excludeClasses className="sun.*"/>
      <!-- etc -->
      ...
    </as:build-signatures>
    ...
  </target>
  ...
</project>
---

* Extending signatures

  In some cases you may want to generate a signature which extends another signature, this is
  achieved by adding the required signatures to the <<<build-signature>>> task. For example:
   
---
<project ... xmlns:as="antlib:org.codehaus.mojo.animal_sniffer">
  ...
  <target ...>
    ...
    <as:build-signatures ...>
      ...
      <signature src="javase-1.5.sig"/>
      <signature src="servlet-2.4.sig"/>
      <!-- etc -->
      ...
    </as:build-signatures>
    ...
  </target>
  ...
</project>
---   
   
  Note: The resulting signature will be the union of:
   
    * any the provided signatures 
    
    * any classes or jar files you provide via <<<path>>> elements
    
    []
    
  In addition, prior to writing the final signatures out:
  
    * (if any <<<includeClasses>>> are specified) only classes which match at least one
      <<<includeClasses>>> rule will be written to the signature.
      
    * (if any <<<excludeClasses>>> are specified) any classes which match at least one
      <<<exlcudeClasses>>> rule will be excluded from the signature. 
      
    * if both <<<includeClasses>>> and <<<excludeClasses>>> rules are specified, the 
      <<<excludeClasses>>> rules over-rule the <<<includeClasses>>> rules.
      
    []
