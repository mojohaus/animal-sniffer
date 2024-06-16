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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main extends ClassFileVisitor {
    private boolean humanReadableName = false;

    private String maximumVersion = "00.0";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java -jar animal-sniffer.jar [JAR/CLASS FILES]");
            System.err.println("  -h   : show a human readable Java version number");
            System.err.println("  -t N : return exit code 1 if any file has a class file version number > N");
            System.exit(-1);
        }

        Main m = new Main();
        String threshold = null;

        List<Path> files = new ArrayList<Path>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                m.humanReadableName = true;
                continue;
            }
            if (args[i].equals("-t")) {
                threshold = args[++i];
                for (Map.Entry<String, String> entry : HUMAN_READABLE_NAME.entrySet()) {
                    if (entry.getValue().equals(threshold)) {
                        threshold = entry.getKey();
                        break;
                    }
                }
                continue;
            }

            files.add(getPath(args[i]));
        }

        for (Path file : files) {
            m.process(file);
        }

        if (threshold != null && m.maximumVersion.compareTo(threshold) > 0) {
            System.exit(1);
        }
    }

    private static Path getPath(String s) {
        try {
            URI uri = new URI(s);
            String scheme = uri.getScheme();
            // Only allow certain schemes to prevent treating (mistyped) file path unintentionally as URI
            if (scheme.equalsIgnoreCase("file") || scheme.equalsIgnoreCase("jrt")) {
                return Paths.get(uri);
            }
            // Fall through
        } catch (URISyntaxException e) {
            // Fall through; probably not a URI but a file path
        }
        return Paths.get(s);
    }

    protected void process(String name, InputStream image) throws IOException {
        DataInputStream dis = new DataInputStream(image);
        byte[] buf = new byte[8];
        dis.readFully(buf);

        String v = u2(buf[6], buf[7]) + "." + u2(buf[4], buf[5]);
        if (maximumVersion.compareTo(v) < 0) {
            maximumVersion = v;
        }

        if (humanReadableName) {
            String hn = HUMAN_READABLE_NAME.get(v);
            if (hn != null) {
                v = hn;
            }
        }

        System.out.println(v + " " + name);
    }

    private static int u2(byte u, byte d) {
        return ((int) u) * 256 + d;
    }

    private static final Map<String, String> HUMAN_READABLE_NAME = new HashMap<>();

    static {
        HUMAN_READABLE_NAME.put("45.0", "Java1");
        HUMAN_READABLE_NAME.put("46.0", "Java2");
        HUMAN_READABLE_NAME.put("47.0", "Java3");
        HUMAN_READABLE_NAME.put("48.0", "Java4");
        HUMAN_READABLE_NAME.put("49.0", "Java5");
        HUMAN_READABLE_NAME.put("50.0", "Java6");
        HUMAN_READABLE_NAME.put("51.0", "Java7");
        HUMAN_READABLE_NAME.put("52.0", "Java8");
        HUMAN_READABLE_NAME.put("53.0", "Java9");
        HUMAN_READABLE_NAME.put("54.0", "Java10");
        HUMAN_READABLE_NAME.put("55.0", "Java11");
        HUMAN_READABLE_NAME.put("56.0", "Java12");
        HUMAN_READABLE_NAME.put("57.0", "Java13");
        HUMAN_READABLE_NAME.put("58.0", "Java14");
        HUMAN_READABLE_NAME.put("59.0", "Java15");
    }
}
