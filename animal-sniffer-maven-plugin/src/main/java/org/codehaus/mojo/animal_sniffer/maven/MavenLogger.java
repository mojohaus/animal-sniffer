package org.codehaus.mojo.animal_sniffer.maven;

/*
 * The MIT License
 *
 * Copyright (c) 2009, codehaus.org
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
import org.apache.maven.plugin.logging.Log;

/**
 * An animal sniffer logger that delegates to a maven log.
 *
 * @author connollys
 * @since 1.3
 */
public final class MavenLogger
    implements Logger
{
    private final Log delegate;

    public MavenLogger( Log delegate )
    {
        this.delegate = delegate;
    }

    public void info( String message )
    {
        delegate.info( message );
    }

    public void info( String message, Throwable t )
    {
        delegate.info( message, t );
    }

    public void debug( String message )
    {
        delegate.debug( message );
    }

    public void debug( String message, Throwable t )
    {
        delegate.debug( message, t );
    }

    public void warn( String message )
    {
        delegate.warn( message );
    }

    public void warn( String message, Throwable t )
    {
        delegate.warn( message, t );
    }

    public void error( String message )
    {
        delegate.error( message );
    }

    public void error( String message, Throwable t )
    {
        delegate.error( message, t );
    }
}
