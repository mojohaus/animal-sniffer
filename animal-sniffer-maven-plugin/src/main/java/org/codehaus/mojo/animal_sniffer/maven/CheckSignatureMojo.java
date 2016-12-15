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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;
import org.codehaus.plexus.util.StringUtils;

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
 */
@Mojo( name = "check", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true )
public class CheckSignatureMojo
    extends AbstractMojo
{

    /**
     * The directory for compiled classes.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    protected File outputDirectory;

    /**
     * Signature module to use.
     */
    @Parameter( required = true, property="animal.sniffer.signature" )
    protected Signature signature;

	/**
	 * @param signatureId
	 *            A fully-qualified path to a signature jar. This allows users
	 *            to set a signature for command-line invocations, such as:
	 *            <p>
	 *            <code>mvn org.codehaus.mojo:animal-sniffer-maven-plugin:1.15:check -Dsignature=org.codehaus.mojo.signature:java17:1.0</code>
	 */
    public void setSignature( String signatureId ) {
		String[] signatureParts = signatureId.split(":");
		if(signatureParts.length == 3) {
			this.signature = new Signature();
			this.signature.setGroupId(signatureParts[0]);
			this.signature.setArtifactId(signatureParts[1]);
			this.signature.setVersion(signatureParts[2]);
		}
    }

    /**
     * Class names to ignore signatures for (wildcards accepted).
     *
     */
    @Parameter
    protected String[] ignores;

    /**
     * Annotation names to consider to ignore annotated methods, classes or fields.
     * <p>
     * By default 'org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement' and
     * 'org.jvnet.animal_sniffer.IgnoreJRERequirement' are used.
     *
     * @see SignatureChecker#ANNOTATION_FQN
     * @see SignatureChecker#PREVIOUS_ANNOTATION_FQN
     */
    @Parameter
    protected String[] annotations;

    /**
     * Should dependencies be ignored.
     *
     */
    @Parameter( defaultValue = "true" )
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
     * @since 1.12
     */
    @Parameter
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
     * @since 1.12
     */
    @Parameter
    private String[] excludeDependencies = null;

    /**
     * Should signature checking be skipped?
     *
     */
    @Parameter( defaultValue = "false", property = "animal.sniffer.skip" )
    protected boolean skip;

    /**
     */
    @Component
    protected ArtifactResolver resolver;

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    /**
     */
    @Parameter( defaultValue = "${localRepository}", readonly=true )
    protected ArtifactRepository localRepository;

    /**
     */
    @Component
    protected ArtifactFactory artifactFactory;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Signature checking is skipped." );
            return;
        }

		if ( signature == null || StringUtils.isBlank(signature.getGroupId()) || signature.getGroupId() == "null") {
			getLog().info( "Signature version is: " + signature.getVersion() );
			return;
        }


        try
        {
            if ( StringUtils.isBlank( signature.getVersion() ) )
            {
                getLog().debug( "Resolving signature " + signature.getGroupId() + ":" + signature.getArtifactId()
                                   + " version from dependencies" );
                String source = "dependencies";
                Dependency match = findMatchingDependency( signature, project.getDependencies() );
                if ( match == null && project.getDependencyManagement() != null )
                {
                    getLog().debug( "Resolving signature " + signature.getGroupId() + ":" + signature.getArtifactId()
                                       + " version from dependencyManagement" );
                    source = "dependencyManagement";
                    match = findMatchingDependency( signature, project.getDependencyManagement().getDependencies() );
                }
                if ( match != null )
                {
                    getLog().info( "Resolved signature " + signature.getGroupId() + ":" + signature.getArtifactId()
                                       + " version as " + match.getVersion() + " from " + source);
                    signature.setVersion( match.getVersion() );
                }
            }

            getLog().info( "Checking unresolved references to " + signature );

            Artifact a = signature.createArtifact( artifactFactory );

            resolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );
            // just check code from this module
            final Set<String> ignoredPackages = buildPackageList();

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
            List<File> sourcePaths = new ArrayList<File>();
            Iterator<String> iterator = project.getCompileSourceRoots().iterator();
            while ( iterator.hasNext() )
            {
                String path = iterator.next();
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

    private static Dependency findMatchingDependency( Signature signature, List<Dependency> dependencies )
    {
        Dependency match = null;
        for ( Iterator<Dependency> iterator = dependencies.iterator(); iterator.hasNext(); )
        {
            Dependency d = iterator.next();
            if ( StringUtils.isBlank( d.getVersion() ) )
            {
                continue;
            }
            if ( StringUtils.equals( d.getGroupId(), signature.getGroupId() ) && StringUtils.equals( d.getArtifactId(),
                                                                                                     signature.getArtifactId() ) )
            {
                if ( "signature".equals( d.getType() ) )
                {
                    // this is a perfect match
                    match = d;
                    break;
                }
                if ( "pom".equals( d.getType() ) )
                {
                    if ( match == null || "jar".equals( match.getType() ) )
                    {
                        match = d;
                    }
                }
                if ( "jar".equals( d.getType() ) )
                {
                    if ( match == null )
                    {
                        match = d;
                    }
                }
            }
        }
        return match;
    }

    /**
     * List of packages defined in the application.
     */
    private Set<String> buildPackageList()
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
            for ( Iterator<Artifact> i = project.getArtifacts().iterator(); i.hasNext(); )
            {

                Artifact artifact = i.next();

                if ( !artifact.getArtifactHandler().isAddedToClasspath() ) {
                    getLog().debug( "Skipping artifact " + BuildSignaturesMojo.artifactId( artifact )
                                        + " as it is not added to the classpath." );
                    continue;
                }

                if ( !( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) || Artifact.SCOPE_PROVIDED.equals(
                    artifact.getScope() ) || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) ) )
                {
                    getLog().debug( "Skipping artifact " + BuildSignaturesMojo.artifactId( artifact )
                                        + " as it is not on the compile classpath." );
                    continue;
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
