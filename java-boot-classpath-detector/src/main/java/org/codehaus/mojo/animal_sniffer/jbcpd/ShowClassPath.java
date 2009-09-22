package org.codehaus.mojo.animal_sniffer.jbcpd;
/*
 * The MIT License
 *
 * Copyright (c) 2009 codehaus.org.
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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

public final class ShowClassPath {

    public static void main(String[] args) {
        String cp = System.getProperty("sun.boot.class.path");
        if (cp != null) {
            System.out.println(cp);
            return;
        }
        cp = System.getProperty("java.boot.class.path");
        if (cp != null) {
            System.out.println(cp);
            return;
        }
        Enumeration i = System.getProperties().propertyNames();
        String name = null;
        while (i.hasMoreElements()) {
            String temp = (String) i.nextElement();
            if (temp.indexOf(".boot.class.path") != -1) {
                if (name == null) {
                    name = temp;
                } else {
                    System.err.println("Cannot auto-detect boot class path " + System.getProperty("java.version"));
                    System.exit(1);
                }
            }
        }
        if (name == null) {
            String version = System.getProperty("java.version");
            if (version.startsWith("1.1.")) {
                // by default, the current directory is added to the classpath
                // we therefore need to strip that out
                cp = System.getProperty("java.class.path");
                cp = removeAll(cp, ".");
                cp = removeAll(cp, new File(".").getAbsolutePath());
                try {
                    cp = removeAll(cp, new File(".").getCanonicalPath());
                } catch (IOException e) {
                    // ignore
                }
                cp = removeAll(cp, new File(".").getAbsolutePath() + System.getProperty("file.separator"));
                try {
                    cp = removeAll(cp, new File(".").getCanonicalPath() + System.getProperty("file.separator"));
                } catch (IOException e) {
                    // ignore
                }
                System.out.println(cp);
                return;
            }
            System.err.println("Cannot auto-detect boot class path " + System.getProperty("java.version") + " " +
                    System.getProperty("java.class.path"));
            System.exit(1);
        }
        System.out.println(System.getProperty(name));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toUpperCase().startsWith("WINDOWS");
    }

    private static String removeAll(String cp, String prefix) {
        String pathSeparator = System.getProperty("path.separator");
        if (cp.startsWith(prefix + pathSeparator)) {
            cp = cp.substring(prefix.length() + pathSeparator.length());
        }
        int j;
        while (-1 != (j = cp.indexOf(pathSeparator + prefix + pathSeparator))) {
            cp = cp.substring(0, j) + cp.substring(j + prefix.length() + pathSeparator.length());
        }
        if (cp.endsWith(pathSeparator + prefix)) {
            cp = cp.substring(0, cp.length() - prefix.length() + pathSeparator.length());
        }
        if (isWindows()) {
            // we might have the prefix or the classpath case differing
            if (cp.toUpperCase().startsWith((prefix + pathSeparator).toUpperCase())) {
                cp = cp.substring(prefix.length() + pathSeparator.length());
            }
            while (-1 != (j = cp.toUpperCase().indexOf((pathSeparator + prefix + pathSeparator).toUpperCase()))) {
                cp = cp.substring(0, j) + cp.substring(j + prefix.length() + pathSeparator.length());
            }
            if (cp.toUpperCase().endsWith((pathSeparator + prefix).toUpperCase())) {
                cp = cp.substring(0, cp.length() - prefix.length() + pathSeparator.length());
            }
        }
        return cp;
    }
}
