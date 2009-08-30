package org.jvnet.animal_sniffer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.HashSet;

/**
 * List up packages seen in the given classes.
 * 
 * @author Kohsuke Kawaguchi
 */
public class PackageListBuilder extends ClassFileVisitor {
    public final Set packages;

    public PackageListBuilder(Set packages) {
        this.packages = packages;
    }

    public PackageListBuilder() {
        this(new HashSet());
    }

    protected void process(String name, InputStream image) throws IOException {
        ClassReader cr = new ClassReader(image);
        cr.accept(new EmptyVisitor() {
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                int idx = name.lastIndexOf('/');
                if(idx<0)   packages.add("");
                else        packages.add(name.substring(0, idx));
            }
        }, 0);
    }
}
