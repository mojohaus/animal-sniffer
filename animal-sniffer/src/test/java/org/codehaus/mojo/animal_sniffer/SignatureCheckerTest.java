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

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.codehaus.mojo.animal_sniffer.logging.Logger;

import junit.framework.TestCase;

public class SignatureCheckerTest extends TestCase
{

    public SignatureCheckerTest( String testName )
    {
        super( testName );
    }

    public void testAnnotationFqn()
    {
        assertEquals( IgnoreJRERequirement.class.getName(), SignatureChecker.ANNOTATION_FQN );
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

    public void testLoadClasses() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream objOut = new ObjectOutputStream(new GZIPOutputStream(out))) {
            objOut.writeObject(new Clazz("my/Class1", Collections.singleton("field1#Ljava/lang/String;"), "java/lang/Object", new String[] {"my/SuperInterface"}));
            objOut.writeObject(new Clazz("my/Class2", Collections.emptySet(), "my/SuperClass", new String[0]));
            objOut.writeObject(null);
        }

        Map<String, Clazz> classes = SignatureChecker.loadClasses(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(2, classes.size());

        Clazz class1 = classes.get("my/Class1");
        assertEquals("my/Class1", class1.getName());
        assertEquals(Collections.singleton("field1#Ljava/lang/String;"), class1.getSignatures());
        assertEquals("java/lang/Object", class1.getSuperClass());
        assertArrayEquals(new String[] {"my/SuperInterface"}, class1.getSuperInterfaces());

        Clazz class2 = classes.get("my/Class2");
        assertEquals("my/Class2", class2.getName());
        assertEquals(Collections.emptySet(), class2.getSignatures());
        assertEquals("my/SuperClass", class2.getSuperClass());
        assertArrayEquals(new String[0], class2.getSuperInterfaces());
    }

    /**
     * Verifies that only certain allowed classes may be deserialized.
     */
    public void testLoadClasses_DisallowedClass() throws Exception
    {
        // Java Serialization data for an instance of DisallowedDummyClass followed by `null`
        byte[] serializationData = {
            31, -117, 8, 0, 0, 0, 0, 0, 0, -1, 37, -63, 65, 14, 64, 48, 16, 0, -64, 37,
            -15, 19, -25, 126, -126, -109, 43, 119, -39, -80, -91, -76, -35, 102, 87, -125, 63, -7, -102, 63,
            56, -104, 121, 94, -88, 84, -96, 99, 89, -52, -60, 51, -83, -104, -43, 4, -34, -40, 96, 116,
            1, -3, -88, -47, 89, 75, 98, 122, -73, 68, 60, -78, 80, -77, -46, -76, -109, 12, -92, 71,
            -35, 58, 69, -17, -7, -92, -71, -51, 33, -36, -115, 71, 85, -8, 21, 37, -64, -107, -46, 7,
            -126, -50, 87, -21, 96, 0, 0, 0,
        };

        try
        {
            SignatureChecker.loadClasses(new ByteArrayInputStream(serializationData));
            fail();
        }
        catch (InvalidClassException e) {
            assertEquals(DisallowedDummyClass.class.getName(), e.classname);
            assertEquals(DisallowedDummyClass.class.getName() + "; Disallowed class for signature data", e.getMessage());
        }


        // Java Serialization data for an instance of a non-existent class followed by `null`
        byte[] missingClassSerializationData = {
            31, -117, 8, 0, 0, 0, 0, 0, 0, -1, 37, -63, -53, 17, 64, 64, 12, 0, -48, 48,
            -93, 19, -25, 20, 97, 75, -64, -39, 100, 8, -69, 62, -119, 73, 24, -118, -46, -102, 30, 28,
            -68, -9, -68, 80, -72, 65, -91, 54, 97, -81, 3, 71, 58, 29, 55, -99, 21, 73, -46, 70,
            107, -25, -110, -58, -111, 13, -21, 52, 9, 29, -89, 113, -120, -36, 47, 108, 13, -5, 81, -74,
            -78, -120, 94, 18, 86, 114, -121, 95, -106, 3, -36, -5, -2, 1, -60, 22, -5, 99, 88, 0,
            0, 0,
        };

        try
        {
            SignatureChecker.loadClasses(new ByteArrayInputStream(missingClassSerializationData));
            fail();
        }
        catch (InvalidClassException e) {
            String className = "org.codehaus.mojo.animal_sniffer.SignatureCheckerTest$UnknownClass";
            assertEquals(className, e.classname);
            assertEquals(className + "; Class not found, probably disallowed class", e.getMessage());
        }
    }

    private static class DisallowedDummyClass implements Serializable
    {
        private static final long serialVersionUID = 1L;

        static {
            // Verify that class is not initialized by test during deserialization
            fail("Must not be initialized");
        }
    }

    /**
     * Uses first {@link SignatureBuilder} to build signature data and then {@link SignatureChecker}
     * to load it.
     */
    public void testLoadClasses_Roundtrip() throws Exception
    {
        /*
         * Bytes for this class:
         * ```
         * package mypackage;
         *
         * interface MyClass {
         *     int i = 1;
         * }
         * ```
         */
        byte[] classBytes = {
            -54, -2, -70, -66, 0, 0, 0, 61, 0, 11, 7, 0, 2, 1, 0, 17, 109, 121, 112, 97,
            99, 107, 97, 103, 101, 47, 77, 121, 67, 108, 97, 115, 115, 7, 0, 4, 1, 0, 16, 106,
            97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 1, 0, 1, 105, 1,
            0, 1, 73, 1, 0, 13, 67, 111, 110, 115, 116, 97, 110, 116, 86, 97, 108, 117, 101, 3,
            0, 0, 0, 1, 1, 0, 10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 12,
            77, 121, 67, 108, 97, 115, 115, 46, 106, 97, 118, 97, 6, 0, 0, 1, 0, 3, 0, 0,
            0, 1, 0, 25, 0, 5, 0, 6, 0, 1, 0, 7, 0, 0, 0, 2, 0, 8, 0, 0,
            0, 1, 0, 9, 0, 0, 0, 2, 0, 10
        };
        Path tempFile = Files.createTempFile("animal-sniffer-test-class", ".class");
        Files.write(tempFile, classBytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SignatureBuilder signatureBuilder = new SignatureBuilder(out, new TestLogger());
        // Use process(File) here because process(Path) does not support single class file at the moment
        signatureBuilder.process(tempFile.toFile());
        signatureBuilder.close();

        Files.delete(tempFile);

        Map<String, Clazz> classes = SignatureChecker.loadClasses(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, classes.size());

        Clazz class1 = classes.get("mypackage/MyClass");
        assertEquals("mypackage/MyClass", class1.getName());
        assertEquals(Collections.singleton("i#I"), class1.getSignatures());
        assertEquals("java/lang/Object", class1.getSuperClass());
        assertArrayEquals(new String[0], class1.getSuperInterfaces());
    }

    static class TestLogger implements Logger
    {
        @Override
        public void debug(String message) {
        }

        @Override
        public void debug(String message, Throwable t) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void info(String message, Throwable t) {
        }

        @Override
        public void warn(String message) {
            fail("Unexpected warning: " + message);
        }

        @Override
        public void warn(String message, Throwable t) {
            if (t != null) {
                t.printStackTrace();
            }
            fail("Unexpected warning: " + message);
        }

        @Override
        public void error(String message) {
            fail("Unexpected error: " + message);
        }

        @Override
        public void error(String message, Throwable t) {
            if (t != null) {
                t.printStackTrace();
            }
            fail("Unexpected error: " + message);
        }
    }
}
