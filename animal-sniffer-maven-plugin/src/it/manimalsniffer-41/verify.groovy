
/*
 * The MIT License
 *
 * Copyright (c) 2012, codehaus.org
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
import org.codehaus.plexus.util.FileUtils;

try
{
    File file = new File( basedir, "build.log" );
    String buf = FileUtils.fileRead( file, "UTF-8" );

    String[] expected = {
        "Bad.java:9: Undefined reference: void java.util.concurrent.DelayQueue.<init>()",
        "Bad.java:9: Undefined reference: java.util.concurrent.ScheduledFuture",
        "Bad.java:11: Undefined reference: boolean java.util.concurrent.ScheduledFuture.isCancelled()",
        "Bad.java:15: Undefined reference: java.util.concurrent.Callable java.util.concurrent.Executors.callable(Runnable)",
        "Bad.java:15: Undefined reference: Object java.util.concurrent.Callable.call()",
        // XXX seems like there should be more of them...
    };
    for (String s : expected) {
        if (!buf.contains(s)) {
            System.err.println("Failed to find: " + s);
            return false;
        }
    }
    if (buf.contains("Good.java")) {
        System.err.println("Should not have warned about Good.java");
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
