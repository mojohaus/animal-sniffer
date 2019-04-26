package org.codehaus.mojo.animal_sniffer.ant;

import java.util.Objects;

/**
 * @author Stephane Nicoll
 */
public class Annotation
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
        if ( !( o instanceof Annotation ) )
        {
            return false;
        }

        Annotation annotation = (Annotation) o;

        return Objects.equals(className, annotation.className);

    }

    public int hashCode()
    {
        return 0;
    }

    public String toString()
    {
        return "Annotation{" + "className='" + className + '\'' + '}';
    }
}
