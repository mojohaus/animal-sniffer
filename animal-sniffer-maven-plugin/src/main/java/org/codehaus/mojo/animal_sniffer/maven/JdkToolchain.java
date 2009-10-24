package org.codehaus.mojo.animal_sniffer.maven;

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

import java.util.Map;

/**
 * Represents the details of the jdk toolchain required.
 *
 * @author Stephen Connolly
 */
public class JdkToolchain
{
    private Map parameters;

    public String getToolchain()
    {
        return "jdk";
    }

    public Map getParameters()
    {
        return parameters;
    }

    public void setParameters( Map parameters )
    {
        this.parameters = parameters;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        JdkToolchain that = (JdkToolchain) o;

        if ( parameters != null ? !parameters.equals( that.parameters ) : that.parameters != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return parameters != null ? parameters.hashCode() : 0;
    }

    public String toString()
    {
        return "JdkToolchain{" + "parameters=" + parameters + '}';
    }
}
