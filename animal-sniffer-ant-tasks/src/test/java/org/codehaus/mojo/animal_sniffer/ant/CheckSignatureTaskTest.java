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
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
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
    public void classAnnotationWithInterspersedResource() throws Exception {
        addFilesetWithResource(ClassSuppressed.class);
        assertSuppressed();
    }

    @Test
    public void methodAnnotationWithInterspersedResource() throws Exception {
        addFilesetWithResource(MethodSuppressed.class);
        assertSuppressed();
    }

    @Test
    public void classAnnotationAcrossPathsWithIgnoredFiles() throws Exception {
        addPathsWithIgnoredFiles(ClassSuppressed.class);
        assertSuppressed();
    }

    @Test
    public void methodAnnotationAcrossPathsWithIgnoredFiles() throws Exception {
        addPathsWithIgnoredFiles(MethodSuppressed.class);
        assertSuppressed();
    }

    @Test
    public void unannotatedClassWithInterspersedResource() throws Exception {
        addFilesetWithResource(Unsuppressed.class);
        assertRejected();
    }

    @Test
    public void unannotatedClassAcrossPathsWithIgnoredFiles() throws Exception {
        addPathsWithIgnoredFiles(Unsuppressed.class);
        assertRejected();
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

    @Test
    public void classAnnotationAcrossDirectories() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, directory("outer-classes", files[1]), directory("i", files[0]));
        assertSuppressed();
    }

    @Test
    public void methodAnnotationAcrossDirectories() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, directory("outer-classes", files[1]), directory("i", files[0]));
        assertSuppressed();
    }

    @Test
    public void classAnnotationAcrossJars() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, jar("outer-classes.jar", files[1]), jar("i.jar", files[0]));
        assertSuppressed();
    }

    @Test
    public void methodAnnotationAcrossJars() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, jar("outer-classes.jar", files[1]), jar("i.jar", files[0]));
        assertSuppressed();
    }

    @Test
    public void classFileBeforeDirectory() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, files[1], directory("i", files[0]));
        assertSuppressed();
    }

    @Test
    public void methodFileBeforeDirectory() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, files[1], directory("i", files[0]));
        assertSuppressed();
    }

    @Test
    public void classDirectoryBeforeFile() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, directory("outer-" + files[0].getName(), files[1]), files[0]);
        assertSuppressed();
    }

    @Test
    public void methodDirectoryBeforeFile() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, directory("outer-" + files[0].getName(), files[1]), files[0]);
        assertSuppressed();
    }

    @Test
    public void classFileBeforeJar() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, files[1], jar("i.jar", files[0]));
        assertSuppressed();
    }

    @Test
    public void methodFileBeforeJar() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, files[1], jar("i.jar", files[0]));
        assertSuppressed();
    }

    @Test
    public void classJarBeforeFile() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, jar("outer-" + files[0].getName() + ".jar", files[1]), files[0]);
        assertSuppressed();
    }

    @Test
    public void methodJarBeforeFile() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, jar("outer-" + files[0].getName() + ".jar", files[1]), files[0]);
        assertSuppressed();
    }

    @Test
    public void classJmodBeforeFile() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, jar("outer-" + files[0].getName() + ".jmod", files[1]), files[0]);
        assertSuppressed();
    }

    @Test
    public void unannotatedClassInJmod() throws Exception {
        File[] files = copyClasses(Unsuppressed.class);
        addFiles(false, files[1], jar("i.jmod", files[0]));
        assertRejected();
    }

    @Test
    public void classFileBeforeDirectoryNamedLikeClass() throws Exception {
        File[] files = copyClasses(ClassSuppressed.class);
        addFiles(false, files[1], directory("i.class", files[0]));
        assertSuppressed();
    }

    @Test
    public void methodFileBeforeDirectoryNamedLikeClass() throws Exception {
        File[] files = copyClasses(MethodSuppressed.class);
        addFiles(true, files[1], directory("i.class", files[0]));
        assertSuppressed();
    }

    @Test
    public void looseFilesStayOnEitherSideOfJar() throws Exception {
        File[] methodFiles = copyClasses(MethodSuppressed.class);
        File[] classFiles = copyClasses(ClassSuppressed.class);
        // Sorting loose files globally would move the longer MethodSuppressed name past its anonymous class's JAR.
        addFiles(true, methodFiles[1], jar("i.jar", methodFiles[0]), classFiles[1], classFiles[0]);
        assertSuppressed();
    }

    private void addFilesetWithResource(Class<?> fixture) throws IOException {
        File[] files = copyClasses(fixture);
        String outerName = files[1].getName();
        File resource = new File(
                files[0].getParentFile(),
                outerName.substring(0, outerName.length() - ".class".length()) + "$2.properties");
        Files.createFile(resource.toPath());
        FileSet fileset = new FileSet();
        fileset.setProject(task.getProject());
        fileset.setDir(files[0].getParentFile());
        Path path = new Path(task.getProject());
        path.addFileset(fileset);
        assertArrayEquals(
                new String[] {files[0].getAbsolutePath(), resource.getAbsolutePath(), files[1].getAbsolutePath()},
                path.list());
        task.addPath(path);
    }

    private void addPathsWithIgnoredFiles(Class<?> fixture) throws IOException {
        File[] files = copyClasses(fixture);
        addFiles(
                true,
                files[0],
                temporaryFolder.newFile("ignored.properties"),
                temporaryFolder.newFile("ignored.zip"),
                temporaryFolder.newFile("ignored.CLASS"),
                temporaryFolder.newFile("README"),
                files[1]);
    }

    private File directory(String name, File file) throws IOException {
        File directory = temporaryFolder.newFolder(name);
        Files.copy(file.toPath(), new File(directory, file.getName()).toPath());
        return directory;
    }

    private File jar(String name, File file) throws IOException {
        File jar = temporaryFolder.newFile(name);
        String packagePath = getClass().getPackage().getName().replace('.', '/');
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry(packagePath + "/" + file.getName()));
            Files.copy(file.toPath(), out);
            out.closeEntry();
        }
        return jar;
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
