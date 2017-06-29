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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ClassFileVisitor
{
    /**
     * Whether to check inside <code>.jar</code> files
     */
    private boolean checkJars = true;

    public boolean isCheckJars()
    {
        return checkJars;
    }

    public void setCheckJars( boolean checkJars )
    {
        this.checkJars = checkJars;
    }

    /**
     * Multi-arg version of {@link #process(File)}.
     */
    public void process( File[] files )
        throws IOException
    {
        Arrays.sort( files, new Comparator<File>()
        {
            public int compare( File f1, File f2 )
            {
                String n1 = f1.getName();
                String n2 = f2.getName();
                // Ensure that outer classes are visited before inner classes:
                int diff = n1.length() - n2.length();
                return diff != 0 ? diff : n1.compareTo( n2 );
            }

        } );
        for ( int i = 0; i < files.length; i++ )
        {
            process( files[i] );
        }
    }

    /**
     * Recursively finds class files and invokes {@link #process(String, InputStream)}
     *
     * @param file Directory full of class files or jar files (in which case all of them are processed recursively),
     *             or a class file (in which case that single class is processed),
     *             or a jar file (in which case all the classes in this jar file are processed.)
     */
    public void process( File file )
        throws IOException
    {
        if ( file.isDirectory() )
        {
            processDirectory( file );
        }
        else if ( file.getName().endsWith( ".class" ) )
        {
            processClassFile( file );
        }
        else if ( file.getName().endsWith( ".jar" ) && checkJars )
        {
            processJarFile( file );
        }

        // ignore other files
    }

    protected void processDirectory( File dir )
        throws IOException
    {
        File[] files = dir.listFiles();
        if ( files == null )
        {
            return;
        }
        process( files );
    }

    protected void processJarFile( File file )
        throws IOException
    {
        try
        {
            JarFile jar = new JarFile( file );
            SortedSet<JarEntry> entries = new TreeSet<JarEntry>( new Comparator<JarEntry>() {
                public int compare( JarEntry e1, JarEntry e2 )
                {
                    String n1 = e1.getName();
                    String n2 = e2.getName();
                    int diff = n1.length() - n2.length();
                    return diff != 0 ? diff : n1.compareTo( n2 );
                }
            } );
            Enumeration<JarEntry> e = jar.entries();
            while ( e.hasMoreElements() )
            {
                JarEntry x = e.nextElement();
                if ( !x.getName().endsWith( ".class" ) )
                {
                    continue;
                }
                entries.add( x );
            }
            Iterator<JarEntry> it = entries.iterator();
            while ( it.hasNext() ) {
                JarEntry x = it.next();
                InputStream is = jar.getInputStream( x );
                try
                {
                    process( file.getPath() + ':' + x.getName(), is );
                }
                finally
                {
                    is.close();
                }
            }
        }
        catch ( IOException cause )
        {
            IOException e = new IOException( " failed to process jar " + file.getPath() + " : " + cause.getMessage() );
            e.initCause( cause );
            throw e;
        }
        catch( Exception cause )
        {
            IOException e = new IOException( " exception while processing jar " + file.getPath() + " : " + cause.getMessage() );
            e.initCause( cause );
            throw e;
        }


    }

    protected void processClassFile( File file )
        throws IOException
    {
        InputStream in = new FileInputStream( file );
        try
        {
            process( file.getPath(), in );
        }
        finally
        {
            in.close();
        }
    }

    /**
     * @param name  Displayable name to identify what class file we are processing
     * @param image Class file image.
     */
    protected abstract void process( String name, InputStream image )
        throws IOException;
}
