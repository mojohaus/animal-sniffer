package org.jvnet.animal_sniffer;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main extends ClassFileVisitor {
    public static void main(String[] args) throws IOException {
        if(args.length==0) {
            System.err.println("Usage: java -jar animal-sniffer.jar [JAR/CLASS FILES]");
            System.exit(-1);
        }

        Main m = new Main();
        for (int i = 0; i < args.length; i++) {
            m.process(new File(args[i]));
        }
    }

    protected void process(String name, InputStream image) throws IOException {
        DataInputStream dis = new DataInputStream(image);
        byte[] buf = new byte[8];
        dis.readFully(buf);

        System.out.println(u2(buf[6],buf[7])+"."+u2(buf[4],buf[5])+" "+name);
    }

    private static int u2(byte u, byte d) {
        return ((int)u)*256+d;
    }
}
