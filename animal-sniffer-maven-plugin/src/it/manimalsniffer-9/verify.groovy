
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

import java.io.*;
import java.util.regex.*;
import org.codehaus.plexus.util.FileUtils;

try
{
    File file = new File( basedir, "build.log" );
    String buf = FileUtils.fileRead( file, "UTF-8" );

    Pattern p = Pattern.compile( "Bad class file .*\\Qicu4j-2.6.1.jar:com/ibm/icu/impl/data/LocaleElements_zh__PINYIN.class\\E" );

    if ( !p.matcher( buf ).find() )
    {
        System.err.println( "User friendly error message not displayed" );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
