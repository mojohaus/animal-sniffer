package org.codehaus.mojo.animal_sniffer.logging;

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

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.PrintStream;

/**
 * Default implementation that sends output to a print writer
 *
 * @author connollys
 * @since 1.3
 */
public final class PrintWriterLogger
    implements Logger
{
    private final PrintStream destination;

    public PrintWriterLogger( PrintStream destination )
    {
        this.destination = destination;
    }

    public void info( String message )
    {
        output( "[INFO]", message, null );
    }

    public void info( String message, Throwable t )
    {
        output( "[INFO]", message, t );
    }

    public void debug( String message )
    {
    }

    public void debug( String message, Throwable t )
    {
    }

    public void warn( String message )
    {
        output( "[WARN]", message, null );
    }

    public void warn( String message, Throwable t )
    {
        output( "[WARN]", message, t );
    }

    public void error( String message )
    {
        output( "[ERROR]", message, null );
    }

    public void error( String message, Throwable t )
    {
        output( "[ERROR]", message, t );
    }

    private void output( String prefix, String message, Throwable t )
    {
        StringWriter w = new StringWriter( );
        PrintWriter pw = new PrintWriter( w );
        pw.print( prefix );
        pw.print( ' ' );
        pw.println( message );
        if ( t != null )
        {
            t.printStackTrace( pw );
        }
        pw.close();
        destination.print( w.toString() );
        destination.flush();
    }
}
