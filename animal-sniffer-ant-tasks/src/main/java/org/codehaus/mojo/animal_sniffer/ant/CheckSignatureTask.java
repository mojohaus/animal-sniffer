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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.resources.FileResource;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.PackageListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
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

    private Vector filesets = new Vector();

    public void setSignature( File signature )
    {
        this.signature = signature;
    }

    public void addFileset( FileSet set )
    {
        filesets.addElement( set );
    }

    public void setClasspath( Path classpath )
    {
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
        if ( this.classpath == null )
        {
            this.classpath = new Path( getProject() );
        }
        this.classpath.createPath().setRefid( r );
    }

    @Override
    public void execute()
        throws BuildException
    {
        if ( signature == null )
        {
            throw new BuildException( "The signature to check must be specified the 'signature' attribute" );
        }
        if ( filesets.isEmpty() )
        {
            log( "Nothing to do", Project.MSG_INFO );
            return;
        }
        try
        {
            log( "Checking unresolved references to " + signature, Project.MSG_INFO );

            if ( !signature.isFile() )
            {
                throw new BuildException( "Could not find signature: " + signature );
            }

            // just check code from this module
            final SignatureChecker signatureChecker =
                new SignatureChecker( new FileInputStream( signature ), buildPackageList(), new AntLogger( this ) );
            Iterator i = filesets.iterator();
            while ( i.hasNext() )
            {
                FileSet fs = (FileSet) i.next();
                DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
                File baseDir = fs.getDir( getProject() );
                final String[] files = ds.getIncludedFiles();
                for ( int j = 0; j < files.length; j++ )
                {
                    signatureChecker.process( new File( baseDir, files[j] ) );
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

    /**
     * List of packages defined in the application.
     */
    private Set buildPackageList()
        throws IOException
    {
        PackageListBuilder plb = new PackageListBuilder();
        apply( plb );
        return plb.packages;
    }

    private void apply( ClassFileVisitor v )
        throws IOException
    {
        for ( Enumeration i = filesets.elements(); i.hasMoreElements(); )
        {
            FileSet fs = (FileSet) i.nextElement();
            DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
            File baseDir = fs.getDir( getProject() );
            final String[] files = ds.getIncludedFiles();
            for ( int j = 0; j < files.length; j++ )
            {
                v.process( new File( baseDir, files[j] ) );
            }
        }
        if ( classpath != null )
        {
            final Iterator i = classpath.createPath().iterator();
            while ( i.hasNext() )
            {
                Object next = i.next();
                if ( next instanceof FileResource )
                {
                    v.process( ( (FileResource) next ).getFile() );
                }
            }
        }
    }
}