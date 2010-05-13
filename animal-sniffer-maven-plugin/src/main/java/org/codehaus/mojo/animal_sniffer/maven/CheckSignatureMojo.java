package org.codehaus.mojo.animal_sniffer.maven;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Checks the classes compiled by this module.
 *
 * @author Kohsuke Kawaguchi
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @goal check
 */
public class CheckSignatureMojo
    extends AbstractMojo
{

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
    protected Signature signature;

    /**
     * Class names to ignore signatures for (wildcards accepted).
     *
     * @parameter
     */
    protected String[] ignores;

    /**
     * Should dependencies be ignored.
     *
     * @parameter default-value="true"
     */
    protected boolean ignoreDependencies;

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

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            getLog().info( "Checking unresolved references to " + signature );

            org.apache.maven.artifact.Artifact a = signature.createArtifact( artifactFactory );

            resolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );
            // just check code from this module
            final Set ignoredPackages = buildPackageList();

            if ( ignores != null )
            {
                for ( int i = 0; i < ignores.length; i++ )
                {
                    String ignore = ignores[i];
                    if ( ignore == null )
                    {
                        continue;
                    }
                    ignoredPackages.add( ignore.replace( '.', '/' ) );
                }
            }

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( ignoredPackages.toString() );
            }

            final SignatureChecker signatureChecker =
                new SignatureChecker( new FileInputStream( a.getFile() ), ignoredPackages,
                                      new MavenLogger( getLog() ) );
            signatureChecker.process( outputDirectory );

            if ( signatureChecker.isSignatureBroken() )
            {
                throw new MojoFailureException(
                    "Signature errors found. Verify them and put @IgnoreJRERequirement on them." );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to check signatures", e );
        }
        catch ( AbstractArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Failed to obtain signature: " + signature, e );
        }
    }

    /**
     * List of packages defined in the application.
     */
    private Set buildPackageList()
        throws IOException
    {
        ClassListBuilder plb = new ClassListBuilder();
        apply( plb );
        return plb.getPackages();
    }

    private void apply( ClassFileVisitor v )
        throws IOException
    {
        v.process( outputDirectory );
        if ( ignoreDependencies )
        {
            Iterator itr = classpathElements.iterator();
            while ( itr.hasNext() )
            {
                String path = (String) itr.next();
                v.process( new File( path ) );
            }
        }
    }
}
