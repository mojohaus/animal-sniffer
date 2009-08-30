package org.jvnet.animal_sniffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ClassFileVisitor {
    /**
     * Multi-arg version of {@link #process(File)}.
     */
    public void process(File[] files) throws IOException {
        for (int i = 0; i < files.length; i++)
            process(files[i]);
    }

    /**
     * Recursively finds class files and invokes {@link #process(String, InputStream)}
     *
     * @param file
     *      Directory full of class files or jar files (in which case all of them are processed recursively),
     *      or a class file (in which case that single class is processed),
     *      or a jar file (in which case all the classes in this jar file are processed.)
     */
    public void process(File file) throws IOException {
        if(file.isDirectory())
            processDirectory(file);
        else
        if(file.getName().endsWith(".class"))
            processClassFile(file);
        else
        if(file.getName().endsWith(".jar"))
            processJarFile(file);

        // ignore other files
    }

    protected void processDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if(files==null) return;
        process(files);
    }

    protected void processJarFile(File file) throws IOException {
        JarFile jar = new JarFile(file);

        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry x =  (JarEntry)e.nextElement();
            if(!x.getName().endsWith(".class")) continue;
            InputStream is = jar.getInputStream(x);
            try {
                process(file.getPath()+':'+x.getName(),is);
            } finally {
                is.close();
            }
        }

    }

    protected void processClassFile(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            process(file.getPath(),in);
        } finally {
            in.close();
        }
    }

    /**
     * @param name
     *      Displayable name to identify what class file we are processing
     * @param image
     *      Class file image.
     */
    protected abstract void process(String name, InputStream image) throws IOException;
}
