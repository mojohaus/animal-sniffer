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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.codehaus.mojo.animal_sniffer.SignatureBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author Kohsuke Kawaguchi
 */
public class BuildSignaturesTask
    extends Task
{

    private File destfile;

    private Vector paths = new Vector();

    public void setDestfile( File dest )
    {
        this.destfile = dest;
    }

    public void addPath( Path path )
    {
        paths.add( path );
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
    }

    public void execute()
        throws BuildException
    {
        validate();
        try
        {
            SignatureBuilder builder = new SignatureBuilder( new FileOutputStream( destfile ), new AntLogger( this ) );
            Iterator i = paths.iterator();
            while ( i.hasNext() )
            {
                Path path = (Path) i.next();
                final String[] files = path.list();
                for ( int j = 0; j < files.length; j++ )
                {
                    log( "Capturing signatures from " + files[j], Project.MSG_INFO );
                    process( builder, new File( files[j] ) );
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
