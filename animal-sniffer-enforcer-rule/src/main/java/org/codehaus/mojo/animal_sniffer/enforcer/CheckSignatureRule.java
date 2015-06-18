package org.codehaus.mojo.animal_sniffer.enforcer;

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
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;
import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since Sep 4, 2009 2:44:29 PM
 */
public class CheckSignatureRule
    implements EnforcerRule
{
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
     * <p>
     * By default {@value SignatureChecker#ANNOTATION_FQN} and
     * {@value SignatureChecker#PREVIOUS_ANNOTATION_FQN} are used.
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
    protected boolean ignoreDependencies = true;

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

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            File outputDirectory = new File( (String) helper.evaluate( "${project.build.outputDirectory}" ) );

            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );

            MavenProject project = (MavenProject) helper.evaluate( "${project}" );

            ArtifactRepository localRepository = (ArtifactRepository) helper.evaluate( "${localRepository}" );

            ArtifactFactory artifactFactory = (ArtifactFactory) helper.getComponent( ArtifactFactory.class );

            if ( StringUtils.isEmpty( signature.getVersion() ) )
            {
                helper.getLog().debug( "Resolving signature " + signature.getGroupId() + ":" + signature.getArtifactId()
                                   + " version from dependencies" );
                String source = "dependencies";
                Dependency match = findMatchingDependency( signature, project.getDependencies() );
                if ( match == null )
                {
                    helper.getLog().debug( "Resolving signature " + signature.getGroupId() + ":" + signature.getArtifactId()
                                       + " version from dependencyManagement" );
                    source = "dependencyManagement";
                    match = findMatchingDependency( signature, project.getDependencyManagement().getDependencies() );
                }
                if ( match != null )
                {
                    helper.getLog().info( "Resolved signature " + signature.getGroupId() + ":" + signature.getArtifactId()
                                       + " version as " + match.getVersion() + " from " + source);
                    signature.setVersion( match.getVersion() );
                }
            }

            helper.getLog().info( "Checking unresolved references to " + signature );

            org.apache.maven.artifact.Artifact a = signature.createArtifact( artifactFactory );

            resolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );
            // just check code from this module

            MavenLogger logger = new MavenLogger( helper.getLog() );

            final Set<String> ignoredPackages = buildPackageList( outputDirectory, project, logger );

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
                new SignatureChecker( new FileInputStream( a.getFile() ), ignoredPackages, logger );
            signatureChecker.setCheckJars( false ); // don't want to descend into jar files that have been copied to
            // the output directory as resources.
            List<File> sourcePaths = new ArrayList<File>();
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
                throw new EnforcerRuleException(
                    "Signature errors found. Verify them and ignore them with the proper annotation if needed." );
            }
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( "Failed to check signatures", e );
        }
        catch ( AbstractArtifactResolutionException e )
        {
            throw new EnforcerRuleException( "Failed to obtain signature: " + signature, e );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
    }

    private static Dependency findMatchingDependency( Signature signature, List<Dependency> dependencies )
    {
        Dependency match = null;
        for ( Dependency d : dependencies )
        {
            if ( StringUtils.isEmpty( d.getVersion() ) )
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
     *
     * @param outputDirectory
     * @param logger
     */
    private Set<String> buildPackageList( File outputDirectory, MavenProject project, Logger logger )
        throws IOException
    {
        ClassListBuilder plb = new ClassListBuilder( logger );
        apply( plb, outputDirectory, project, logger );
        return plb.getPackages();
    }

    private void apply( ClassFileVisitor v, File outputDirectory, MavenProject project, Logger logger )
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

            logger.debug( "Building list of classes from dependencies" );
            for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
            {

                Artifact artifact = (Artifact) i.next();

                if ( !artifact.getArtifactHandler().isAddedToClasspath() ) {
                    logger.debug( "Skipping artifact " + artifactId( artifact )
                                        + " as it is not added to the classpath." );
                    continue;
                }

                if ( !( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) || Artifact.SCOPE_PROVIDED.equals(
                    artifact.getScope() ) || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) ) )
                {
                    logger.debug( "Skipping artifact " + artifactId( artifact )
                                        + " as it is not on the compile classpath." );
                    continue;
                }

                if ( includesFilter != null && !includesFilter.include( artifact ) )
                {
                    logger.debug( "Skipping classes in artifact " + artifactId( artifact )
                                        + " as it does not match include rules." );
                    continue;
                }

                if ( excludesFilter != null && !excludesFilter.include( artifact ) )
                {
                    logger.debug( "Skipping classes in artifact " + artifactId( artifact )
                                        + " as it does matches exclude rules." );
                    continue;
                }

                logger.debug( "Adding classes in artifact " + artifactId( artifact ) +
                                    " to the ignores" );
                v.process( artifact.getFile() );
            }
        }
    }

    public boolean isCacheable()
    {
        return false;
    }

    public boolean isResultValid( EnforcerRule enforcerRule )
    {
        return false;
    }

    public String getCacheId()
    {
        return getClass().getName() + new Random().nextLong();
    }

    private static String artifactId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + (
            artifact.getClassifier() != null ? ":" + artifact.getClassifier() : "" ) + ":" + artifact.getBaseVersion();

    }
}
