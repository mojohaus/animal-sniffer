package org.codehaus.mojo.animal_sniffer;

/*
 * The MIT License
 *
 * Copyright (c) 2008 Kohsuke Kawaguchi and codehaus.org.
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
 *
 */

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Checks the signature against classes in this list.
 *
 * @author Kohsuke Kawaguchi
 */
public class SignatureChecker
    extends ClassFileVisitor
{
    private final Map/*<String, Clazz>*/ classes = new HashMap();

    private final Logger logger;

    /**
     * Classes in this packages are considered to be resolved elsewhere and
     * thus not a subject of the error checking when referenced.
     */
    private final Set ignoredPackages;

    private boolean hadError = false;

    public static void main( String[] args )
        throws Exception
    {
        Set ignoredPackages = new HashSet();
        ignoredPackages.add( "org/jvnet/animal_sniffer" );
        ignoredPackages.add( "org/codehaus/mojo/animal_sniffer" );
        ignoredPackages.add( "org/objectweb/*" );
        new SignatureChecker( new FileInputStream( "signature" ), ignoredPackages, new PrintWriterLogger( System.out ) ).process(
            new File( "target/classes" ) );
    }

    public SignatureChecker( InputStream in, Set ignoredPackages, Logger logger )
        throws IOException
    {
        this.ignoredPackages = ignoredPackages;
        this.logger = logger;
        try
        {
            ObjectInputStream ois = new ObjectInputStream( new GZIPInputStream( in ) );
            while ( true )
            {
                Clazz c = (Clazz) ois.readObject();
                if ( c == null )
                {
                    return; // finished
                }
                classes.put( c.name, c );
            }
        }
        catch ( ClassNotFoundException e )
        {
            throw new NoClassDefFoundError( e.getMessage() );
        }
    }

    protected void process( final String name, InputStream image )
        throws IOException
    {
        ClassReader cr = new ClassReader( image );

        final Set warned = new HashSet();

        cr.accept( new EmptyVisitor()
        {
            public MethodVisitor visitMethod( int access, String name, String desc, String signature,
                                              String[] exceptions )
            {
                return new EmptyVisitor()
                {
                    /**
                     * True if @IgnoreJRERequirement is set.
                     */
                    boolean ignoreError = false;

                    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
                    {
                        if ( desc.equals( "Lorg/jvnet/animal_sniffer/IgnoreJRERequirement;" ) )
                        {
                            ignoreError = true;
                        }
                        if ( desc.equals( "Lorg/codehaus/mojo/animal_sniffer/IgnoreJRERequirement;" ) )
                        {
                            ignoreError = true;
                        }
                        return super.visitAnnotation( desc, visible );
                    }

                    public void visitMethodInsn( int opcode, String owner, String name, String desc )
                    {
                        check( owner, name + desc );
                    }

                    public void visitTypeInsn( int opcode, String type )
                    {
                        if ( shouldBeIgnored( type ) )
                        {
                            return;
                        }
                        if ( type.startsWith( "[" ) )
                        {
                            return; // array
                        }
                        Clazz sigs = (Clazz) classes.get( type );
                        if ( sigs == null )
                        {
                            error( "Undefined reference: " + type );
                        }
                    }

                    public void visitFieldInsn( int opcode, String owner, String name, String desc )
                    {
                        check( owner, name + '#' + desc );
                    }

                    private void check( String owner, String sig )
                    {
                        if ( shouldBeIgnored( owner ) )
                        {
                            return;
                        }
                        if ( find( (Clazz) classes.get( owner ), sig ) )
                        {
                            return; // found it
                        }
                        error( "Undefined reference: " + owner + '.' + sig );
                    }

                    private boolean shouldBeIgnored( String type )
                    {
                        if ( ignoreError )
                        {
                            return true;    // warning suppressed in this context
                        }
                        if ( type.startsWith( "[" ) )
                        {
                            return true; // array
                        }
                        int idx = type.lastIndexOf( '/' );
                        if ( idx < 0 )
                        {
                            return ignoredPackages.contains( "" );
                        }
                        String pkg = type.substring( 0, idx );
                        if ( ignoredPackages.contains( pkg ) )
                        {
                            return true;
                        }

                        // check wildcard form
                        while ( true )
                        {
                            if ( ignoredPackages.contains( pkg + "/*" ) )
                            {
                                return true;
                            }
                            idx = pkg.lastIndexOf( '/' );
                            if ( idx < 0 )
                            {
                                return false;
                            }
                            pkg = pkg.substring( 0, idx );
                        }
                    }
                };
            }

            /**
             * If the given signature is found in the specified class, return true.
             */
            private boolean find( Clazz c, String sig )
            {
                if ( c == null )
                {
                    return false;
                }
                if ( c.signatures.contains( sig ) )
                {
                    return true;
                }

                if ( sig.startsWith( "<" ) )
                // constructor and static initializer shouldn't go up the inheritance hierarchy
                {
                    return false;
                }

                if ( find( (Clazz) classes.get( c.superClass ), sig ) )
                {
                    return true;
                }

                if ( c.superInterfaces != null )
                {
                    for ( int i = 0; i < c.superInterfaces.length; i++ )
                    {
                        if ( find( (Clazz) classes.get( c.superInterfaces[i] ), sig ) )
                        {
                            return true;
                        }
                    }
                }

                return false;
            }

            private void error( String msg )
            {
                hadError = true;
                if ( warned.add( msg ) )
                {
                    logger.error( msg + " in " + name );
                }
            }
        }, 0 );
    }

    public boolean isSignatureBroken()
    {
        return hadError;
    }
}
