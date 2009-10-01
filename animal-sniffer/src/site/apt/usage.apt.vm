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
 Animal Sniffer CLI
 -----
 Kohsuke Kawaguchi
 -----
 2008-11-15
 -----

What is this?

    This simple command line tool looks at Java class files and determine
    the format version number. The common use case of this tool is to figure
    out what is the offending jar file when you see <<<UnsupportedClassVersionError>>>.

Usage

    This tool can accept any number of:

    * Class files

    * Jar files
    
    * Directories

    []
    
    When directories are given, they are recursively scanned for class files and jar files.
    When jar files are given, class files in it is examined.

    The tool produces output like the following, so use the grep command to filter out the list:

-----------------------------
% java -jar target/animal-sniffer-1.0-SNAPSHOT.jar tmp
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/AbstractCreator.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/AbstractCreatorProcessor.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/AbstractProcessor.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/AttributesHolder.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/FragmentedArray.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/MutableXMLStreamBuffer.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/XMLStreamBuffer.class
50.0 tmp/streambuffer-0.7.jar:com/sun/xml/stream/buffer/XMLStreamBufferException.class
-----------------------------

