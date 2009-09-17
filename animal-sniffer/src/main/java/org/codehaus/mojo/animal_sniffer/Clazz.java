package org.codehaus.mojo.animal_sniffer;

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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a class signature.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Clazz
    implements Serializable
{
    /**
     * The name of the class.
     */
    private final String name;

    /**
     * The set of methods and constants that form the signature of the class.
     */
    private final Set signatures;

    /**
     * The superclass of the class.
     */
    private final String superClass;

    /**
     * The list of interfaces implemented by the class.
     */
    private final String[] superInterfaces;

    /**
     * Creates a new class signature.
     *
     * @param name            the name of the class.
     * @param signatures      the signatures.
     * @param superClass      the superclass.
     * @param superInterfaces the interfaces implemented by the class.
     */
    public Clazz( String name, Set signatures, String superClass, String[] superInterfaces )
    {
        this.name = name;
        this.signatures = signatures;
        this.superClass = superClass;
        this.superInterfaces = (String[]) superInterfaces.clone();
    }

    /**
     * Merges two class instances.
     *
     * @param defA the first instance.
     * @param defB the second instance
     * @throws ClassCastException if the two instances have different names or if the superclasses differ.
     */
    public Clazz( Clazz defA, Clazz defB )
    {
        if ( !defA.name.equals( defB.name ) )
        {
            // nothing we can do... this is an invalid argument
            throw new ClassCastException( "Cannot merge different classes: " + defA.name + " and " + defB.name );
        }
        if ( !defA.superClass.equals( defB.superClass ) )
        {
            // nothing we can do... this is a breaking change
            throw new ClassCastException( "Cannot merger class " + defB.name + " as it has changed superclass:" );
        }
        Set superInterfaces = new HashSet();
        if ( defA.superInterfaces != null )
        {
            superInterfaces.addAll( Arrays.asList( defA.superInterfaces ) );
        }
        if ( defB.superInterfaces != null )
        {
            superInterfaces.addAll( Arrays.asList( defB.superInterfaces ) );
        }
        Set signatures = new HashSet();
        signatures.addAll( defA.signatures );
        signatures.addAll( defB.signatures );
        this.name = defA.getName();
        this.signatures = signatures;
        this.superClass = defA.superClass;
        this.superInterfaces = (String[]) superInterfaces.toArray( new String[superInterfaces.size()] );
    }

    public String getName()
    {
        return name;
    }

    public Set getSignatures()
    {
        return signatures;
    }

    public String getSuperClass()
    {
        return superClass;
    }

    public String[] getSuperInterfaces()
    {
        return superInterfaces;
    }

    private static final long serialVersionUID = 1L;

}
