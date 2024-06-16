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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Builds up a signature list from the given classes.
 *
 * @author Kohsuke Kawaguchi
 */
public class SignatureBuilder extends ClassFileVisitor {
    private boolean foundSome;

    private final Logger logger;

    private List<Pattern> includeClasses;

    private List<Pattern> excludeClasses;

    private final Map<String, Clazz> classes = new TreeMap<>();

    public static void main(String[] args) throws IOException {
        SignatureBuilder builder =
                new SignatureBuilder(new FileOutputStream("signature"), new PrintWriterLogger(System.out));
        if (getJavaVersion() > 8) {
            builder.process(Paths.get(URI.create("jrt:/modules")));
        } else {
            builder.process(new File(System.getProperty("java.home"), "lib/rt.jar"));
        }
        builder.close();
    }

    private final ObjectOutputStream oos;

    public SignatureBuilder(OutputStream out, Logger logger) throws IOException {
        this(null, out, logger);
    }

    public void addInclude(String className) {
        if (includeClasses == null) {
            includeClasses = new ArrayList<>();
        }
        includeClasses.add(RegexUtils.compileWildcard(className));
    }

    public void addExclude(String className) {
        if (excludeClasses == null) {
            excludeClasses = new ArrayList<>();
        }
        excludeClasses.add(RegexUtils.compileWildcard(className));
    }

    public SignatureBuilder(InputStream[] ins, OutputStream out, Logger logger) throws IOException {
        this.logger = logger;
        if (ins != null) {
            for (InputStream in : ins) {
                try (ObjectInputStream ois = new SignatureObjectInputStream(new GZIPInputStream(in))) {
                    while (true) {
                        Clazz c = (Clazz) ois.readObject();
                        if (c == null) {
                            break; // finished
                        }
                        classes.put(c.getName(), c);
                    }
                } catch (ClassNotFoundException e) {
                    throw new IOException("Could not read base signatures", e);
                }
            }
        }
        oos = new ObjectOutputStream(new GZIPOutputStream(out));
    }

    public void close() throws IOException {
        int count = 0;
        for (Map.Entry<String, Clazz> entry : classes.entrySet()) {
            final String className = entry.getKey().replace('/', '.');
            if (includeClasses != null) {
                boolean included = false;
                for (Pattern p : includeClasses) {
                    included |= p.matcher(className).matches();
                }
                if (!included) {
                    continue;
                }
            }
            if (excludeClasses != null) {
                boolean excluded = false;
                for (Pattern p : excludeClasses) {
                    excluded |= p.matcher(className).matches();
                }
                if (excluded) {
                    continue;
                }
            }
            count++;
            logger.debug(className);

            oos.writeObject(entry.getValue());
        }
        oos.writeObject(null); // EOF marker
        logger.info("Wrote signatures for " + count + " classes.");
        oos.close();
        if (!foundSome) {
            throw new IOException("No index is written");
        }
    }

    @Override
    protected void process(String name, InputStream image) throws IOException {
        logger.debug(name);
        foundSome = true;
        ClassReader cr = new ClassReader(image);
        SignatureVisitor v = new SignatureVisitor();
        cr.accept(v, 0);
        v.end();
    }

    private class SignatureVisitor extends ClassVisitor {
        private Clazz clazz;

        public SignatureVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(
                int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.clazz = new Clazz(name, superName, interfaces);
        }

        public void end() throws IOException {
            Clazz cur = classes.get(clazz.getName());
            if (cur == null) {
                classes.put(clazz.getName(), clazz);
            } else {
                classes.put(clazz.getName(), new Clazz(clazz, cur));
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            clazz.getSignatures().add(name + desc);
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            clazz.getSignatures().add(name + "#" + desc);
            return null;
        }
    }

    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        // Allow these formats:
        // 1.8.0_72-ea
        // 9-ea
        // 9
        // 9.0.1
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0, dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }
}
