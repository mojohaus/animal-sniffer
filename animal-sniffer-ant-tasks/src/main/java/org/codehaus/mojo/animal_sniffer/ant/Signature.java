package org.codehaus.mojo.animal_sniffer.ant;
/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Stephen Connolly
 * @since 07-Sep-2009 19:58:20
 */
public class Signature
{
    private File src;

    public File getSrc()
    {
        return src;
    }

    public void setSrc( File src )
    {
        this.src = src;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof Signature ) )
        {
            return false;
        }

        Signature signature = (Signature) o;

        if ( src != null ? !src.equals( signature.src ) : signature.src != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return src != null ? src.hashCode() : 0;
    }

    public String toString()
    {
        return "Signature{" + "src=" + src + '}';
    }
}
