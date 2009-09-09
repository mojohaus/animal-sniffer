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

import org.codehaus.mojo.animal_sniffer.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Merges signature files.
 *
 * @author Stephen Connolly
 */
public class SignatureMerger
{
    private final Map/*<String, Clazz>*/ classes = new HashMap();

    private final Logger logger;

    public static void main( String[] args )
        throws Exception
    {
        // TODO add command arg parsing
//        new SignatureMerger( new FileInputStream( "signature" ), ignoredPackages,
//                             new PrintWriterLogger( System.out ) ).process( new File( "target/classes" ) );
    }

    public SignatureMerger( InputStream[] in, OutputStream out, Logger logger )
        throws IOException
    {
        this.logger = logger;
        for ( int i = 0; i < in.length; i++ )
        {
            try
            {
                ObjectInputStream ois = new ObjectInputStream( new GZIPInputStream( in[i] ) );
                while ( true )
                {
                    Clazz c = (Clazz) ois.readObject();
                    if ( c == null )
                    {
                        return; // finished
                    }
                    Clazz cur = (Clazz) classes.get( c.getName() );
                    if ( cur == null )
                    {
                        classes.put( c.getName(), c );
                    }
                    else
                    {
                        classes.put( c.getName(), new Clazz( c, cur ) );
                    }
                }
            }
            catch ( ClassNotFoundException e )
            {
                throw new NoClassDefFoundError( e.getMessage() );
            }
        }
        ObjectOutputStream oos = new ObjectOutputStream( new GZIPOutputStream( out ) );
        Iterator i = classes.entrySet().iterator();
        while ( i.hasNext() )
        {
            Map.Entry entry = (Map.Entry) i.next();
            logger.info( (String) entry.getKey() );
            oos.writeObject( entry.getValue() );
        }
        oos.writeObject( null );   // EOF marker
        oos.close();
    }

}