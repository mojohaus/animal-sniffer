package org.codehaus.mojo.animal_sniffer.ant;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.codehaus.mojo.animal_sniffer.SignatureBuilder;

/**
 * @author Kohsuke Kawaguchi
 */
public class BuildSignaturesTask
    extends Task
{

    private File destfile;

    private Vector<Path> paths = new Vector<>();

    private Vector<Signature> signatures = new Vector<>();

    private Vector<Ignore> includeClasses = new Vector<>();

    private Vector<Ignore> excludeClasses = new Vector<>();

    public void setDestfile( File dest )
    {
        this.destfile = dest;
    }

    public void addPath( Path path )
    {
        paths.add( path );
    }

    public Signature createSignature()
    {
        Signature signature = new Signature();
        signatures.add( signature );
        return signature;
    }

    public Ignore createIncludeClasses()
    {
        final Ignore result = new Ignore();
        includeClasses.add( result );
        return result;
    }

    public Ignore createExcludeClasses()
    {
        final Ignore result = new Ignore();
        excludeClasses.add( result );
        return result;
    }

    protected void validate()
    {
        if ( destfile == null )
        {
            throw new BuildException( "destfile not set" );
        }
        if ( paths.size() < 1 )
        {
            throw new BuildException( "path not set" );
        }
        for ( Signature signature : signatures )
        {
            if ( signature.getSrc() == null )
            {
                throw new BuildException( "signature src not set" );
            }
            if ( !signature.getSrc().isFile() )
            {
                throw new BuildException( "signature " + signature.getSrc() + " does not exist" );
            }
        }
        for ( Ignore tmp : includeClasses )
        {
            if ( tmp.getClassName() == null )
            {
                throw new BuildException( "includeClasses className not set" );
            }
        }
        for ( Ignore tmp : excludeClasses )
        {
            if ( tmp.getClassName() == null )
            {
                throw new BuildException( "excludeClasses className not set" );
            }
        }

    }

    public void execute()
        throws BuildException
    {
        validate();
        try
        {
            Vector<InputStream> inStreams = new Vector<>();
            for ( Signature signature : signatures )
            {
                log( "Importing signatures from " + signature.getSrc() );
                inStreams.add( new FileInputStream( signature.getSrc() ) );
            }

            SignatureBuilder builder =
                new SignatureBuilder( inStreams.toArray( new InputStream[0] ),
                                      new FileOutputStream( destfile ), new AntLogger( this ) );
            for ( Ignore tmp: includeClasses )
            {
                builder.addInclude( tmp.getClassName() );
            }
            for ( Ignore tmp : excludeClasses )
            {
                builder.addExclude( tmp.getClassName() );
            }
            for ( Path path : paths )
            {
                final String[] files = path.list();
                for ( String file : files )
                {
                    log( "Capturing signatures from " + file, Project.MSG_INFO );
                    process( builder, new File( file ) );
                }
            }
            builder.close();
        }
        catch ( IOException e )
        {
            throw new BuildException( e );
        }
    }

    private void process( SignatureBuilder builder, File f )
        throws IOException
    {
        if ( f.exists() )
        {
            builder.process( f );
        }
    }
}
