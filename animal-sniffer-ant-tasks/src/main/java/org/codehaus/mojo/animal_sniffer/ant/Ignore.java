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

import java.lang.Object;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Stephen Connolly
 * @since 07-Sep-2009 16:55:55
 */
public class Ignore
{
    private String className;

    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof Ignore ) )
        {
            return false;
        }

        Ignore ignore = (Ignore) o;

        if ( className != null ? !className.equals( ignore.className ) : ignore.className != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return 0;
    }

    public String toString()
    {
        return "Ignore{" + "className='" + className + '\'' + '}';
    }
}
