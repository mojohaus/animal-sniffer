/*
 * The MIT License
 *
 * Copyright 2012 Codehaus.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.codehaus.mojo.animal_sniffer;

import junit.framework.TestCase;

public class SignatureCheckerTest extends TestCase
{

    public SignatureCheckerTest( String testName )
    {
        super( testName );
    }

    public void testToSourceForm()
    {
        assertSourceForm( "java.util.HashMap", "java/util/HashMap", null );
        assertSourceForm( "java.util.Map.Entry", "java/util/Map$Entry", null );
        assertSourceForm( "String", "java/lang/String", null );
        assertSourceForm( "java.lang.reflect.Field", "java/lang/reflect/Field", null );
        assertSourceForm( "Thread.State", "java/lang/Thread$State", null );
        assertSourceForm( "java.util.Set my.Class.myfield", "my/Class", "myfield#Ljava/util/Set;" );
        assertSourceForm( "String[] my.Class.myfield", "my/Class", "myfield#[Ljava/lang/String;" );
        assertSourceForm( "double[][][] my.Class.myfield", "my/Class", "myfield#[[[D" );
        assertSourceForm( "void my.Class.mymethod()", "my/Class", "mymethod()V" );
        assertSourceForm( "Object my.Class.mymethod(int, double, Thread)", "my/Class",
                          "mymethod(IDLjava/lang/Thread;)Ljava/lang/Object;" );
    }

    public void testToAnnotationDescriptor()
    {
        assertAnnotationDescriptor( "Lorg/codehaus/mojo/animal_sniffer/IgnoreJRERequirement;",
                                    "org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement" );
        assertAnnotationDescriptor( "Lorg/jvnet/animal_sniffer/IgnoreJRERequirement;",
                                    "org.jvnet.animal_sniffer.IgnoreJRERequirement" );

        assertAnnotationDescriptor( "Lcom/foo/Bar;", "com.foo.Bar" );
        assertAnnotationDescriptor( "Lcom/foo/Bar;", "com/foo/Bar" );
    }

    private static void assertSourceForm( String expected, String type, String sig )
    {
        assertEquals( expected, SignatureChecker.toSourceForm( type, sig ) );
    }

    private static void assertAnnotationDescriptor( String expected, String fqn )
    {
        assertEquals( expected, SignatureChecker.toAnnotationDescriptor( fqn ) );
    }

}
