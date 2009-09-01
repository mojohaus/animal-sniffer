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

/**
 * Abstracts out logging so that the different logging providers (eg ANT or Maven) can be plugged in.
 *
 * @author connollys
 * @since 1.3
 */
public interface Logger
{
    void info( String message );

    void info( String message, Throwable t );

    void debug( String message );

    void debug( String message, Throwable t );

    void warn( String message );

    void warn( String message, Throwable t );

    void error( String message );

    void error( String message, Throwable t );
}
