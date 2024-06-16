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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ClassFileVisitor {

    protected final Logger logger;

    protected ClassFileVisitor() {
        this(new PrintWriterLogger(System.err));
    }

    protected ClassFileVisitor(Logger logger) {
        this.logger = logger;
    }

    /**
     * Whether to check inside <code>.jar</code> files
     */
    private boolean checkJars = true;

    public boolean isCheckJars() {
        return checkJars;
    }

    public void setCheckJars(boolean checkJars) {
        this.checkJars = checkJars;
    }

    /**
     * Multi-arg version of {@link #process(File)}.
     */
    public void process(File[] files) throws IOException {
        Arrays.sort(files, (f1, f2) -> {
            String n1 = f1.getName();
            String n2 = f2.getName();
            // Ensure that outer classes are visited before inner classes:
            int diff = n1.length() - n2.length();
            return diff != 0 ? diff : n1.compareTo(n2);
        });
        for (File f : files) {
            process(f);
        }
    }

    /**
     * Recursively finds class files and invokes {@link #process(String, InputStream)}
     *
     * @param file Directory full of class files or jar files (in which case all of them are processed recursively),
     *             or a class file (in which case that single class is processed),
     *             or a jar file (in which case all the classes in this jar file are processed.)
     */
    public void process(File file) throws IOException {
        if (file.isDirectory()) {
            processDirectory(file);
        } else if (file.getName().endsWith(".class")) {
            processClassFile(file);
        } else if ((file.getName().endsWith(".jar") || file.getName().endsWith(".jmod")) && checkJars) {
            processJarFile(file);
        }

        // ignore other files
    }

    /**
     * Recursively finds class files and invokes {@link #process(String, InputStream)}
     *
     * @param path Directory (or other Path like {@code Paths.get(URI.create("jrt:/modules"))}) full of class files
     */
    public void process(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            final SortedSet<Path> files = new TreeSet<>();

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".class")) {
                    files.add(file);
                }
                // XXX we could add processing of jars here as well
                // but it's not necessary for processing: Paths.get(URI.create("jrt:/modules"))
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                for (final Path file : files) {
                    try (final InputStream inputStream = Files.newInputStream(file)) {
                        process(file.toString(), inputStream);
                    }
                }
                files.clear();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected void processDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        process(files);
    }

    protected void processJarFile(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            SortedSet<JarEntry> entries = new TreeSet<>((e1, e2) -> {
                String n1 = e1.getName();
                String n2 = e2.getName();
                int diff = n1.length() - n2.length();
                return diff != 0 ? diff : n1.compareTo(n2);
            });
            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry x = e.nextElement();
                String name = x.getName();
                if (!name.endsWith(".class")) {
                    continue; // uninteresting to log even at debug
                }
                if (name.startsWith("META-INF/") || name.equals("module-info.class")) {
                    logger.debug("Ignoring " + name);
                    continue;
                }
                entries.add(x);
            }
            for (JarEntry x : entries) {
                // Even debug level seems too verbose for: logger.debug( "Processing " + x.getName() + " in " + file );
                try (InputStream is = jar.getInputStream(x)) {
                    process(file.getPath() + ':' + x.getName(), is);
                }
            }

        } catch (IOException cause) {
            throw new IOException(" failed to process jar " + file.getPath() + " : " + cause.getMessage(), cause);
        }
    }

    protected void processClassFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            process(file.getPath(), in);
        }
    }

    /**
     * @param name  Displayable name to identify what class file we are processing
     * @param image Class file image.
     */
    protected abstract void process(String name, InputStream image) throws IOException;
}
