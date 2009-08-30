package org.jvnet.animal_sniffer.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jvnet.animal_sniffer.SignatureBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class BuildSignatureTask extends Task {

    private File dest;
    private File javaHome;

    public void setDest(File dest) {
        this.dest = dest;
    }

    public void setJavaHome(File javaHome) {
        this.javaHome = javaHome;
    }

    public void execute() throws BuildException {
        try {
            SignatureBuilder builder = new SignatureBuilder(new FileOutputStream(dest));
            process(builder,"lib/rt.jar");
            process(builder,"lib/jce.jar");
            process(builder,"lib/jsse.jar");
            builder.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void process(SignatureBuilder builder, String name) throws IOException {
        if(javaHome==null)
            javaHome = new File(System.getProperty("java.home"));
        File f = new File(javaHome, name);
        if(f.exists())
            builder.process(f);
    }
}
