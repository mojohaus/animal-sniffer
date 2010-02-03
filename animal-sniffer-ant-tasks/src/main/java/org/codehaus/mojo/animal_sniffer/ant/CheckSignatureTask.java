package org.codehaus.mojo.animal_sniffer.ant;

/*
 * The MIT License
 *
 * Copyright (c) 2009, codehaus.org.
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.resources.FileResource;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Checks the files against the provided signature.
 *
 * @author connollys
 * @since 1.3
 */
public class CheckSignatureTask
    extends Task
{

    private File signature;

    private Path classpath;

    private Vector paths = new Vector();

    private Vector ignores = new Vector();

    public void addPath( Path path )
    {
        paths.add( path );
    }

    public Ignore createIgnore( )
    {
        final Ignore result = new Ignore();
        ignores.add( result );
        return result;
    }

    public void setSignature( File signature )
    {
        this.signature = signature;
    }

    public Path createClasspath()
    {
        log( "In createClasspath", Project.MSG_INFO );
        if ( this.classpath == null )
        {
            this.classpath = new Path( getProject() );
        }
        return this.classpath.createPath();
    }

    public void setClasspath( Path classpath )
    {
        log( "In setClasspath", Project.MSG_INFO );
        if ( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    public void setClasspathRef( Reference r )
    {
        log( "In setClasspathRef", Project.MSG_INFO );
        createClasspath().setRefid( r );
    }

    public void execute()
        throws BuildException
    {
        validate();
        try
        {
            log( "Checking unresolved references to " + signature, Project.MSG_INFO );

            if ( !signature.isFile() )
            {
                throw new BuildException( "Could not find signature: " + signature );
            }

            final Set ignoredPackages = buildPackageList();

            Iterator i = ignores.iterator();
            while ( i.hasNext() )
            {
                Ignore ignore = (Ignore) i.next();
                if (ignore == null|| ignore.getClassName()== null) continue;
                ignoredPackages.add( ignore.getClassName().replace( '.', '/' ) );
            }

            final SignatureChecker signatureChecker =
                new SignatureChecker( new FileInputStream( signature ), ignoredPackages, new AntLogger( this ) );

            i = paths.iterator();
            while ( i.hasNext() )
            {
                Path path = (Path) i.next();
                final String[] files = path.list();
                for ( int j = 0; j < files.length; j++ )
                {
                    signatureChecker.process( new File( files[j] ) );
                }
            }

            if ( signatureChecker.isSignatureBroken() )
            {
                throw new BuildException( "Signature errors found. Verify them and put @IgnoreJRERequirement on them.",
                                          getLocation() );
            }
        }
        catch ( IOException e )
        {
            throw new BuildException( "Failed to check signatures", e );
        }
    }

    protected void validate()
    {
        if ( signature == null )
        {
            throw new BuildException( "signature not set" );
        }
        if ( paths.size() < 1 )
        {
            throw new BuildException( "path not set" );
        }

    }

    /**
     * List of packages defined in the application.
     */
    private Set buildPackageList()
        throws IOException
    {
        ClassListBuilder plb = new ClassListBuilder();
        apply( plb );
        return plb.getPackages();
    }

    private void apply( ClassFileVisitor v )
        throws IOException
    {
        Iterator i = paths.iterator();
        while ( i.hasNext() )
        {
            Path path = (Path) i.next();
            final String[] files = path.list();
            for ( int j = 0; j < files.length; j++ )
            {
                log( "Ignoring the signatures from file to be checked: " + files[j], Project.MSG_INFO );
                v.process( new File( files[j] ) );
            }
        }
        if ( classpath != null )
        {
            i = classpath.iterator();
            while ( i.hasNext() )
            {
                Object next = i.next();
                if ( next instanceof FileResource )
                {
                    final File file = ( (FileResource) next ).getFile();
                    log( "Ignoring the signatures from classpath: " + file, Project.MSG_INFO );
                    v.process( file );
                }
            }
        }
    }
}