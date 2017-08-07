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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * List up classes seen.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClassListBuilder
    extends ClassFileVisitor
{
    private final Set<String> packages;

    public Set<String> getPackages()
    {
        return packages;
    }

    public ClassListBuilder( Set<String> packages, Logger logger )
    {
        super( logger );
        this.packages = packages;
    }

    public ClassListBuilder( Logger logger )
    {
        this( new HashSet<String>(), logger );
    }

    protected void process( String name, InputStream image )
        throws IOException
    {
        try
        {
            ClassReader cr = new ClassReader( image );
            cr.accept( new ClassVisitor(Opcodes.ASM5)
            {
                public void visit( int version, int access, String name, String signature, String superName,
                                   String[] interfaces )
                {
                    packages.add( name.replace( '/', '.' ) );
                }
            }, 0 );
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            logger.error( "Bad class file " + name );
            // MANIMALSNIFFER-9 it is a pity that ASM does not throw a nicer error on encountering a malformed
            // class file.
            IOException ioException = new IOException( "Bad class file " + name );
            ioException.initCause( e );
            throw ioException;
        }
        catch ( IllegalArgumentException e )
        {
            logger.error( "Bad class file " + name );
            IOException ioException = new IOException( "Bad class file " + name );
            ioException.initCause( e );
            throw ioException;
        }
    }
}
