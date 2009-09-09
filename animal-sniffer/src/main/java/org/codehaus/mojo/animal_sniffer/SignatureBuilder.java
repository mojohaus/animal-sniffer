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

import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Builds up a signature list from the given classes.
 *
 * @author Kohsuke Kawaguchi
 */
public class SignatureBuilder
    extends ClassFileVisitor
{
    private boolean foundSome;

    private final Logger logger;

    private final Map classes = new HashMap();

    public static void main( String[] args )
        throws IOException
    {
        SignatureBuilder builder =
            new SignatureBuilder( new FileOutputStream( "signature" ), new PrintWriterLogger( System.out ) );
        builder.process( new File( System.getProperty( "java.home" ), "lib/rt.jar" ) );
        builder.close();
    }

    private final ObjectOutputStream oos;

    public SignatureBuilder( OutputStream out, Logger logger )
        throws IOException
    {
        this( null, out, logger );
    }

    public SignatureBuilder( InputStream[] in, OutputStream out, Logger logger )
        throws IOException
    {
        this.logger = logger;
        if ( in != null )
        {
            for ( int i = 0; i < in.length; i++ )
            {
                ObjectInputStream ois = new ObjectInputStream( new GZIPInputStream( in[i] ) );
                try
                {
                    while ( true )
                    {
                        Clazz c = (Clazz) ois.readObject();
                        if ( c == null )
                        {
                            break; // finished
                        }
                        classes.put( c.getName(), c );
                    }
                }
                catch ( ClassNotFoundException e )
                {
                    final IOException ioException = new IOException( "Could not read base signatures" );
                    ioException.initCause( e );
                    throw ioException;
                }
                finally
                {
                    ois.close();
                }
            }
        }
        oos = new ObjectOutputStream( new GZIPOutputStream( out ) );
    }

    public void close()
        throws IOException
    {
        Iterator i = classes.entrySet().iterator();
        while ( i.hasNext() )
        {
            Map.Entry entry = (Map.Entry) i.next();
            logger.info( (String) entry.getKey() );
            oos.writeObject( entry.getValue() );
        }
        oos.writeObject( null );   // EOF marker
        oos.close();
        if ( !foundSome )
        {
            throw new IOException( "No index is written" );
        }
    }

    protected void process( String name, InputStream image )
        throws IOException
    {
        logger.info( name );
        foundSome = true;
        ClassReader cr = new ClassReader( image );
        SignatureVisitor v = new SignatureVisitor();
        cr.accept( v, 0 );
        v.end();
    }

    private class SignatureVisitor
        extends EmptyVisitor
    {
        private Clazz clazz;

        public void visit( int version, int access, String name, String signature, String superName,
                           String[] interfaces )
        {
            this.clazz = new Clazz( name, new HashSet(), superName, interfaces );
        }

        public void end()
            throws IOException
        {
            Clazz cur = (Clazz) classes.get( clazz.getName() );
            if ( cur == null )
            {
                classes.put( clazz.getName(), clazz );
            }
            else
            {
                classes.put( clazz.getName(), new Clazz( clazz, cur ) );
            }
        }

        public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions )
        {
            clazz.getSignatures().add( name + desc );
            return null;
        }

        public FieldVisitor visitField( int access, String name, String desc, String signature, Object value )
        {
            clazz.getSignatures().add( name + "#" + desc );
            return null;
        }
    }
}
