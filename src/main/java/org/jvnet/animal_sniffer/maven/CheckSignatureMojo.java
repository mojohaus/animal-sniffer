package org.jvnet.animal_sniffer.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.*;
import org.apache.maven.project.MavenProject;
import org.jvnet.animal_sniffer.SignatureChecker;
import org.jvnet.animal_sniffer.PackageListBuilder;
import org.jvnet.animal_sniffer.ClassFileVisitor;

import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Checks the classes compiled by this module.
 *
 * @author Kohsuke Kawaguchi
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @goal check
 */
public class CheckSignatureMojo extends AbstractMojo {

    /**
     * Project classpath.
     *
     * @parameter expression="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    protected List classpathElements;

    /**
     * The directory for compiled classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    protected File outputDirectory;

    /**
     * Signature module to use.
     *
     * @required
     * @parameter
     */
    protected Artifact signature;

    /**
     * @component
     * @readonly
     */
    protected ArtifactResolver resolver;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${localRepository}"
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     * @readonly
     */
    protected ArtifactFactory artifactFactory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Checking unresolved references to "+signature);
            final boolean[] hadError = new boolean[1];

            org.apache.maven.artifact.Artifact a = signature.createArtifact(artifactFactory);
            resolver.resolve(a,project.getRemoteArtifactRepositories(), localRepository);
            // just check code from this module
            new SignatureChecker(new FileInputStream(a.getFile()),buildPackageList()) {
                protected void reportError(String msg) {
                    hadError[0] = true;
                    getLog().error(msg);
                }

                protected void process(String name, InputStream image) throws IOException {
                    getLog().debug(name);
                    super.process(name, image);
                }
            }.process(outputDirectory);

            if(hadError[0])
                throw new MojoExecutionException("Signature errors found. Verify them and put @IgnoreJRERequirement on them.");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to check signatures",e);
        } catch (AbstractArtifactResolutionException e) {
            throw new MojoExecutionException("Failed to obtain signature: "+signature,e);
        }
    }

    /**
     * List of packages defined in the application.
     */
    private Set buildPackageList() throws IOException {
        PackageListBuilder plb = new PackageListBuilder();
        apply(plb);
        return plb.packages;
    }

    private void apply(ClassFileVisitor v) throws IOException {
        v.process(outputDirectory);
        for (Iterator itr = classpathElements.iterator(); itr.hasNext();) {
            String path = (String) itr.next();
            v.process(new File(path));
        }
    }
}
