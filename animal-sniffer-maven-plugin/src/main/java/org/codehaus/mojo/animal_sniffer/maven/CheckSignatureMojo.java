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
import org.codehaus.mojo.animal_sniffer.Clazz;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks the classes compiled by this module.
 *
 * @author Kohsuke Kawaguchi
 */
@Mojo( name = "check", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class CheckSignatureMojo
    extends AbstractMojo
{

    /**
     * The directory for compiled classes.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    protected File outputDirectory;

    /**
     * The directory for compiled test classes.
     *
     * @since 1.19
     */
    @Parameter( defaultValue = "${project.build.testOutputDirectory}", required = true, readonly = true )
    protected File testOutputDirectory;

    /**
     * Should test classes be checked.
     *
     * @since 1.19
     */
    @Parameter( property = "animal.sniffer.checkTestClasses", defaultValue = "false" )
    protected boolean checkTestClasses;

    /**
     * Signature module to use.
     */
    @Parameter( required = true, property = "animal.sniffer.signature" )
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
     * Should signature check failures throw an error?
     *
     */
    @Parameter( defaultValue = "true", property = "animal.sniffer.failOnError" )
    protected boolean failOnError;

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

    static Map<File, Map<String, Clazz>> classes = new ConcurrentHashMap<>();

    @Override
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
                for ( String ignore : ignores )
                {
                    if ( ignore == null )
                    {
                        continue;
                    }
                    ignoredPackages.add( ignore.replace( '.', '/' ) );
                }
            }

            final SignatureChecker signatureChecker =
                new SignatureChecker( loadClasses( a.getFile() ), ignoredPackages,
                                      new MavenLogger( getLog() ) );
            signatureChecker.setCheckJars( false ); // don't want to decend into jar files that have been copied to
                                                    // the output directory as resources.

            signatureChecker.setSourcePath( buildSourcePathList() );

            if ( annotations != null )
            {
                signatureChecker.setAnnotationTypes( Arrays.asList( annotations ) );
            }

            if ( checkTestClasses )
            {
                signatureChecker.process( new File[] { outputDirectory, testOutputDirectory } );
            }
            else
            {
                signatureChecker.process( outputDirectory );
            }

            if ( signatureChecker.isSignatureBroken() )
            {
                if (failOnError)
                {
                        throw new MojoFailureException(
                            "Signature errors found. Verify them and ignore them with the proper annotation if needed." );
                }
                else
                {
                    getLog().info(
                    "Signature errors found. Verify them and ignore them with the proper annotation if needed." );
                }
            } else {
                getLog().debug( "No signature errors" );
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

    private static Map<String, Clazz> loadClasses( File f ) throws IOException
    {
        Map<String, Clazz> classes = CheckSignatureMojo.classes.get( f );
        if ( classes == null )
        {
            classes = SignatureChecker.loadClasses( new FileInputStream( f ) );
            CheckSignatureMojo.classes.putIfAbsent( f, classes );
        }
        return classes;
    }

    private static Dependency findMatchingDependency( Signature signature, List<Dependency> dependencies )
    {
        Dependency match = null;
        for ( Dependency d : dependencies )
        {
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
        if ( checkTestClasses )
        {
            v.process( testOutputDirectory );
        }
        if ( ignoreDependencies )
        {
            PatternIncludesArtifactFilter includesFilter = includeDependencies == null
                ? null
                : new PatternIncludesArtifactFilter( Arrays.asList( includeDependencies ) );
            PatternExcludesArtifactFilter excludesFilter = excludeDependencies == null
                ? null
                : new PatternExcludesArtifactFilter( Arrays.asList( excludeDependencies ) );

            getLog().debug( "Building list of classes from dependencies" );

            Set<String> classpathScopes = new HashSet<>(
                Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM ) );
            if ( checkTestClasses )
            {
                classpathScopes.addAll( Arrays.asList( Artifact.SCOPE_TEST, Artifact.SCOPE_RUNTIME ) );
            }

            for ( Artifact artifact : (Iterable<Artifact>) project.getArtifacts() )
            {

                if ( !artifact.getArtifactHandler().isAddedToClasspath() )
                {
                    getLog().debug( "Skipping artifact " + BuildSignaturesMojo.artifactId( artifact )
                                        + " as it is not added to the classpath." );
                    continue;
                }

                if ( !classpathScopes.contains( artifact.getScope() ) )
                {
                    getLog().debug(
                        "Skipping artifact " + BuildSignaturesMojo.artifactId( artifact ) + " as it is not on the " + (
                            checkTestClasses
                                ? "test"
                                : "compile" ) + " classpath." );
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

                getLog().debug(
                    "Adding classes in artifact " + BuildSignaturesMojo.artifactId( artifact ) + " to the ignores" );
                v.process( artifact.getFile() );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<File> buildSourcePathList( )
    {
        List<String> compileSourceRoots = new ArrayList<>( project.getCompileSourceRoots() );
        if ( checkTestClasses )
        {
            compileSourceRoots.addAll( project.getTestCompileSourceRoots() );
        }
        List<File> sourcePathList = new ArrayList<>( compileSourceRoots.size() );
        for ( String compileSourceRoot : compileSourceRoots)
        {
            sourcePathList.add( new File( compileSourceRoot ) );
        }
        return sourcePathList;
    }
}
