package org.codehaus.mojo.animal_sniffer.ant;

/*
 * The MIT License
 *
 * Copyright (c) 2026, mojohaus.org.
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
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CheckSignatureTaskTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CheckSignatureTask task;
    private ByteArrayOutputStream errors;

    @Before
    public void setUp() throws Exception {
        File signature = temporaryFolder.newFile("empty.signature");
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(signature)))) {
            out.writeObject(null);
        }

        errors = new ByteArrayOutputStream();
        DefaultLogger logger = new DefaultLogger();
        logger.setErrorPrintStream(new PrintStream(errors, true, "UTF-8"));
        logger.setMessageOutputLevel(Project.MSG_ERR);
        Project project = new Project();
        project.addBuildListener(logger);

        task = new CheckSignatureTask();
        task.setProject(project);
        task.setSignature(signature);
        task.createIgnore().setClassName("java.lang.*");
        task.createAnnotation().setClassName(IgnoreJRERequirement.class.getName());
    }

    @Test
    public void classAnnotationWithReversedFiles() throws Exception {
        addFiles(false, copyClasses(ClassSuppressed.class));
        assertSuppressed();
    }

    @Test
    public void methodAnnotationWithReversedFiles() throws Exception {
        addFiles(false, copyClasses(MethodSuppressed.class));
        assertSuppressed();
    }

    @Test
    public void classAnnotationAcrossPaths() throws Exception {
        addFiles(true, copyClasses(ClassSuppressed.class));
        assertSuppressed();
    }

    @Test
    public void methodAnnotationAcrossPaths() throws Exception {
        addFiles(true, copyClasses(MethodSuppressed.class));
        assertSuppressed();
    }

    @Test
    public void classAnnotationWithOuterFileFirst() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, files[1], files[0]);
        assertSuppressed();
    }

    @Test
    public void methodAnnotationWithOuterFileFirst() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(false, files[1], files[0]);
        assertSuppressed();
    }

    @Test
    public void unannotatedClassWithReversedFiles() throws Exception {
        addFiles(false, copyClasses(Unsuppressed.class));
        assertRejected();
    }

    @Test
    public void unannotatedClassAcrossPaths() throws Exception {
        addFiles(true, copyClasses(Unsuppressed.class));
        assertRejected();
    }

    @Test
    public void classAnnotationInDirectory() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, files[0].getParentFile());
        assertSuppressed();
    }

    @Test
    public void classAnnotationInJar() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        File jar = temporaryFolder.newFile("classes.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            for (File file : files) {
                out.putNextEntry(new JarEntry(file.getName()));
                Files.copy(file.toPath(), out);
                out.closeEntry();
            }
        }
        addFiles(false, jar);
        assertSuppressed();
    }

    private File[] copyClasses(Class<?> fixture) throws IOException {
        File directory = temporaryFolder.newFolder();
        String resource = fixture.getName().replace('.', '/');
        String filename = resource.substring(resource.lastIndexOf('/') + 1);
        // Deliberately put the anonymous class before its annotated enclosing class or method.
        String[] suffixes = {"$1.class", ".class"};
        File[] files = new File[suffixes.length];
        for (int i = 0; i < suffixes.length; i++) {
            files[i] = new File(directory, filename + suffixes[i]);
            try (InputStream in = fixture.getResourceAsStream("/" + resource + suffixes[i])) {
                assertNotNull(in);
                Files.copy(in, files[i].toPath());
            }
        }
        return files;
    }

    private void addFiles(boolean separatePaths, File... files) {
        Path path = new Path(task.getProject());
        for (File file : files) {
            path.setLocation(file);
            if (separatePaths) {
                task.addPath(path);
                path = new Path(task.getProject());
            }
        }
        if (!separatePaths) {
            task.addPath(path);
        }
    }

    private void assertSuppressed() throws Exception {
        task.execute();
        assertEquals("", errors.toString("UTF-8"));
    }

    private void assertRejected() throws Exception {
        try {
            task.execute();
            fail("Expected an undefined reference in the unannotated anonymous class");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Signature errors found."));
        }
        String output = errors.toString("UTF-8");
        assertTrue(output, output.contains("Undefined reference: java.util.List java.util.Collections.emptyList()"));
    }

    @IgnoreJRERequirement
    public static class ClassSuppressed {
        public static Runnable create() {
            return new Runnable() {
                @Override
                public void run() {
                    Collections.emptyList();
                }
            };
        }
    }

    public static class MethodSuppressed {
        @IgnoreJRERequirement
        public static Runnable create() {
            return new Runnable() {
                @Override
                public void run() {
                    Collections.emptyList();
                }
            };
        }
    }

    public static class Unsuppressed {
        public static Runnable create() {
            return new Runnable() {
                @Override
                public void run() {
                    Collections.emptyList();
                }
            };
        }
    }
}
