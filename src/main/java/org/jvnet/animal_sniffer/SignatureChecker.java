package org.jvnet.animal_sniffer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

/**
 * Checks the signature against classes in this list.
 * @author Kohsuke Kawaguchi
 */
public class SignatureChecker extends ClassFileVisitor {
    private final Map/*<String, Clazz>*/ classes = new HashMap();

    /**
     * Classes in this packages are considered to be resolved elsewhere and
     * thus not a subject of the error checking when referenced.
     */
    private final Set ignoredPackages;

    public static void main(String[] args) throws Exception {
        Set ignoredPackages = new HashSet();
        ignoredPackages.add("org/jvnet/animal_sniffer");
        ignoredPackages.add("org/objectweb/*");
        new SignatureChecker(new FileInputStream("signature"),ignoredPackages).process(new File("target/classes"));
    }

    public SignatureChecker(InputStream in, Set ignoredPackages) throws IOException {
        this.ignoredPackages = ignoredPackages;
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(in));
            while(true) {
                Clazz c = (Clazz) ois.readObject();
                if(c==null)    return; // finished
                classes.put(c.name,c);
            }
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    protected void process(final String name, InputStream image) throws IOException {
        ClassReader cr = new ClassReader(image);

        final Set warned = new HashSet();

        cr.accept(new EmptyVisitor() {
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new EmptyVisitor() {
                    /**
                     * True if @IgnoreJRERequirement is set.
                     */
                    boolean ignoreError = false;

                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if(desc.equals("Lorg/jvnet/animal_sniffer/IgnoreJRERequirement;"))
                            ignoreError = true;
                        return super.visitAnnotation(desc, visible);
                    }

                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        check(owner, name + desc);
                    }

                    public void visitTypeInsn(int opcode, String type) {
                        if(shouldBeIgnored(type))   return;
                        if(type.startsWith("["))    return; // array
                        Clazz sigs = (Clazz) classes.get(type);
                        if(sigs==null)
                            error("Undefined reference: "+type);
                    }

                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        check(owner, name + '#' + desc);
                    }

                    private void check(String owner, String sig) {
                        if(shouldBeIgnored(owner))   return;
                        if (find((Clazz) classes.get(owner), sig))
                            return; // found it
                        error("Undefined reference: "+owner+'.'+sig);
                    }

                    private boolean shouldBeIgnored(String type) {
                        if(ignoreError)             return true;    // warning suppressed in this context
                        if(type.startsWith("["))    return true; // array
                        int idx = type.lastIndexOf('/');
                        if(idx<0)   return ignoredPackages.contains("");
                        String pkg = type.substring(0, idx);
                        if(ignoredPackages.contains(pkg))
                            return true;

                        // check wildcard form
                        while(true) {
                            if(ignoredPackages.contains(pkg+"/*"))
                                return true;
                            idx=pkg.lastIndexOf('/');
                            if(idx<0)   return false;
                            pkg = pkg.substring(0,idx);
                        }
                    }
                };
            }

            /**
             * If the given signature is found in the specified class, return true.
             */
            private boolean find(Clazz c, String sig) {
                if(c==null)     return false;
                if(c.signatures.contains(sig))  return true;

                if(sig.startsWith("<"))
                    // constructor and static initializer shouldn't go up the inheritance hierarchy
                    return false;

                if(find((Clazz) classes.get(c.superClass),sig))   return true;

                if(c.superInterfaces!=null)
                    for (int i = 0; i < c.superInterfaces.length; i++)
                        if(find((Clazz) classes.get(c.superInterfaces[i]),sig))
                            return true;

                return false;
            }

            private void error(String msg) {
                if(warned.add(msg))
                    reportError(msg+" in "+name);
            }
        }, 0);
    }

    protected void reportError(String msg) {
        System.err.println(msg);
    }
}
