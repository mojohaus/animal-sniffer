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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * @threadSafe
 */
public class CheckSignatureMojo
    extends AbstractMojo
{

    /**
     * The directory for compiled classes.
     *
     * @parameter property="project.build.outputDirectory"
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
     * Annotation names to consider to ignore annotated methods, classes or fields.
     * <p/>
     * By default 'org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement' and
     * 'org.jvnet.animal_sniffer.IgnoreJRERequirement' are used.
     *
     * @parameter
     * @see SignatureChecker#ANNOTATION_FQN
     * @see SignatureChecker#PREVIOUS_ANNOTATION_FQN
     */
    protected String[] annotations;

    /**
     * Should dependencies be ignored.
     *
     * @parameter default-value="true"
     */
    protected boolean ignoreDependencies;

    /**
     * A list of artifact patterns to include. Patterns can include <code>*</code> as a wildcard match for any
     * <b>whole</b> segment, valid patterns are:
     * <ul>
     * <li><code>groupId:artifactId</code></li>
     * <li><code>groupId:artifactId:type</code></li>
     * <li><code>groupId:artifactId:type:version</code></li>
     * <li><code>groupId:artifactId:type:classifier</code></li>
     * <li><code>groupId:artifactId:type:classifier:version</code></li>
     * </ul>
     *
     * @parameter
     * @since 1.12
     */
    private String[] includeDependencies = null;

    /**
     * A list of artifact patterns to exclude. Patterns can include <code>*</code> as a wildcard match for any
     * <b>whole</b> segment, valid patterns are:
     * <ul>
     * <li><code>groupId:artifactId</code></li>
     * <li><code>groupId:artifactId:type</code></li>
     * <li><code>groupId:artifactId:type:version</code></li>
     * <li><code>groupId:artifactId:type:classifier</code></li>
     * <li><code>groupId:artifactId:type:classifier:version</code></li>
     * </ul>
     *
     * @parameter
     * @since 1.12
     */
    private String[] excludeDependencies = null;

    /**
     * Should signature checking be skipped?
     *
     * @parameter default-value="false" property="animal.sniffer.skip"
     */
    protected boolean skip;

    /**
     * @component
     * @readonly
     */
    protected ArtifactResolver resolver;

    /**
     * @parameter property="project"
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter property="localRepository"
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
        if ( skip )
        {
            getLog().info( "Signature checking is skipped." );
            return;
        }

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

            final SignatureChecker signatureChecker =
                new SignatureChecker( new FileInputStream( a.getFile() ), ignoredPackages,
                                      new MavenLogger( getLog() ) );
            signatureChecker.setCheckJars( false ); // don't want to decend into jar files that have been copied to
                                                    // the output directory as resources.
            List sourcePaths = new ArrayList();
            Iterator iterator = project.getCompileSourceRoots().iterator();
            while ( iterator.hasNext() )
            {
                String path = (String) iterator.next();
                sourcePaths.add( new File( path ) );
            }
            signatureChecker.setSourcePath( sourcePaths );

            if ( annotations != null )
            {
                signatureChecker.setAnnotationTypes( Arrays.asList( annotations ) );
            }

            signatureChecker.process( outputDirectory );

            if ( signatureChecker.isSignatureBroken() )
            {
                throw new MojoFailureException(
                    "Signature errors found. Verify them and ignore them with the proper annotation if needed." );
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
        ClassListBuilder plb = new ClassListBuilder( new MavenLogger( getLog() ) );
        apply( plb );
        return plb.getPackages();
    }

    private void apply( ClassFileVisitor v )
        throws IOException
    {
        v.process( outputDirectory );
        if ( ignoreDependencies )
        {
            PatternIncludesArtifactFilter includesFilter = includeDependencies == null
                ? null
                : new PatternIncludesArtifactFilter( Arrays.asList( includeDependencies ) );
            PatternExcludesArtifactFilter excludesFilter = excludeDependencies == null
                ? null
                : new PatternExcludesArtifactFilter( Arrays.asList( excludeDependencies ) );

            getLog().debug( "Building list of classes from dependencies" );
            for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
            {

                Artifact artifact = (Artifact) i.next();

                if ( !artifact.getArtifactHandler().isAddedToClasspath() ) {
                    getLog().debug( "Skipping artifact " + BuildSignaturesMojo.artifactId( artifact )
                                        + " as it is not added to the classpath." );
                }

                if ( includesFilter != null && !includesFilter.include( artifact ) )
                {
                    getLog().debug( "Skipping classes in artifact " + BuildSignaturesMojo.artifactId( artifact )
                                        + " as it does not match include rules." );
                    continue;
                }

                if ( excludesFilter != null && !excludesFilter.include( artifact ) )
                {
                    getLog().debug( "Skipping classes in artifact " + BuildSignaturesMojo.artifactId( artifact )
                                        + " as it does matches exclude rules." );
                    continue;
                }

                getLog().debug( "Adding classes in artifact " + BuildSignaturesMojo.artifactId( artifact ) +
                                    " to the ignores" );
                v.process( artifact.getFile() );
            }
        }
    }
}
