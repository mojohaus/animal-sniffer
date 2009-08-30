package org.codehaus.mojo.animal_sniffer;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class Clazz
    implements Serializable
{
    public final String name;

    public final Set signatures;

    public final String superClass;

    public final String[] superInterfaces;

    public Clazz( String name, Set signatures, String superClass, String[] superInterfaces )
    {
        this.name = name;
        this.signatures = signatures;
        this.superClass = superClass;
        this.superInterfaces = superInterfaces;
    }

    private static final long serialVersionUID = 1L;
}
