package org.codehaus.mojo.animal_sniffer.maven;

/*
 * The MIT License
 *
 * Copyright (c) 2009, codehaus.org
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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.JavaToolChain;
import org.codehaus.mojo.animal_sniffer.SignatureBuilder;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Connolly
 * @goal build
 */
public class BuildSignaturesMojo
    extends AbstractMojo
{
    /**
     * Should the signatures from java home be included.
     *
     * @parameter expression="${includeJavaHome}" default-value="true"
     * @since 1.3
     */
    private boolean includeJavaHome;

    /**
     * Should no signatures be generated if no java home is available.
     *
     * @parameter expression="${skipIfNoJavaHome}" default-value="false"
     * @since 1.3
     */
    private boolean skipIfNoJavaHome;

    /**
     * Should the signatures from this module's classes be included..
     *
     * @parameter expression="${includeJavaHome}" default-value="true"
     * @since 1.3
     */
    private boolean includeModuleClasses;

    /**
     * Classes to generate signatures of.
     *
     * @parameter
     * @since 1.3
     */
    private String[] includeClasses = null;

    /**
     * Classes to exclude from generating signatures of.
     *
     * @parameter
     * @since 1.3
     */
    private String[] excludeClasses = null;

    /**
     * A list of artifact patterns to include. Follows the pattern
     * "groupId:artifactId:type:classifier:version".
     *
     * @parameter
     * @since 1.3
     */
    private String[] includeDependencies = null;

    /**
     * A list of artifact patterns to exclude. Follows the pattern
     * "groupId:artifactId:type:classifier:version".
     *
     * @parameter
     * @since 1.3
     */
    private String[] excludeDependencies = null;

    /**
     * A list of signatures to include.
     *
     * @parameter
     * @since 1.3
     */
    private Signature[] includeSignatures = null;

    /**
     * The java home to generate the signatures of, if not specified only the signatures of dependencies will be
     * included.
     *
     * @parameter expression="${javaHome}"
     * @since 1.3
     */
    private String javaHome;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     * @since 1.3
     */
    private File outputDirectory;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @since 1.3
     */
    private File classesDirectory;

    /**
     * The name of the generated signatures.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     * @since 1.3
     */
    private String signaturesName;

    /**
     * The classifier to add to the generated signatures.
     *
     * @parameter
     * @since 1.3
     */
    private String classifier;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private ToolchainManager toolchainManager;

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter default-value="jdk"
     */
    private String toolchain;

    /**
     * @parameter
     */
    private Map toolchainParams;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        //get toolchain from context
        Toolchain tc = null;
        if ( toolchainManager != null && toolchain != null && toolchainParams != null )
        {
            try
            {
                final ToolchainPrivate[] tcp = getToolchains( toolchain );
                for ( int i = 0; i < tcp.length; i++ )
                {
                    if ( tcp[i].matchesRequirements( toolchainParams ) )
                    {
                        tc = tcp[i];
                        break;
                    }
                }
            }
            catch ( MisconfiguredToolchainException e )
            {
                throw new MojoExecutionException( e.getLocalizedMessage(), e );
            }

        }
        if ( tc == null && toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", //NOI18N
                                                                session );
        }
        if ( tc != null )
        {
            if ( tc instanceof JavaToolChain )
            {
                getLog().info( "Toolchain in animal-sniffer-maven-plugin: " + tc );

                //when the executable to use is explicitly set by user in mojo's parameter, ignore toolchains.
                if ( javaHome != null )
                {
                    getLog().warn( "Toolchains are ignored, 'javaHome' parameter is set to " + javaHome );
                }
                else
                {
                    //assign the path to executable from toolchains
                    javaHome = ( (JavaToolChain) tc ).findTool( "../jre" ); //NOI18N
                }
            }
        }

        if ( includeJavaHome && javaHome == null )
        {
            if ( skipIfNoJavaHome )
            {
                getLog().warn( "Skipping signature generation as could not find java home" );
                return;
            }
            throw new MojoFailureException(
                "Cannot include java home if java home is not specified (either via javaHome or toolchains)" );
        }

        if ( includeJavaHome && !new File( javaHome ).isDirectory() )
        {
            if ( skipIfNoJavaHome )
            {
                getLog().warn( "Skipping signature generation as java home (" + javaHome + ") does not exist" );
                return;
            }
            throw new MojoFailureException( "Cannot include java home if specified java home does not exist" );
        }

        File sigFile = getTargetFile( outputDirectory, signaturesName, classifier, "signature" );
        try
        {
            outputDirectory.mkdirs();
            List baseSignatures = new ArrayList();
            for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                if ( StringUtils.equals( "signature", artifact.getType() ) )
                {
                    getLog().info( "Importing sigantures from " + artifact.getFile() );
                    baseSignatures.add( new FileInputStream( artifact.getFile() ) );
                }

            }
            SignatureBuilder builder =
                new SignatureBuilder( (InputStream[]) baseSignatures.toArray( new InputStream[baseSignatures.size()] ),
                                      new FileOutputStream( sigFile ), new MavenLogger( getLog() ) );
            if ( classesDirectory.isDirectory() && includeModuleClasses )
            {
                getLog().info( "Parsing sigantures from " + classesDirectory );
                builder.process( classesDirectory );
            }

            PatternIncludesArtifactFilter includesFilter = includeDependencies == null
                ? null
                : new PatternIncludesArtifactFilter( Arrays.asList( includeDependencies ) );
            PatternExcludesArtifactFilter excludesFilter = excludeDependencies == null
                ? null
                : new PatternExcludesArtifactFilter( Arrays.asList( excludeDependencies ) );

            for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                boolean result = true;

                if ( includesFilter != null && !includesFilter.include( artifact ) )
                {
                    getLog().debug(
                        "Artifact " + artifactId( artifact ) + " ignored as it does not match include rules." );
                    continue;
                }

                if ( excludesFilter != null && !excludesFilter.include( artifact ) )
                {
                    getLog().debug(
                        "Artifact " + artifactId( artifact ) + " ignored as it does matches exclude rules." );
                    continue;
                }

                if ( StringUtils.equals( "jar", artifact.getType() ) )
                {
                    getLog().info( "Parsing sigantures from " + artifactId( artifact ) );
                    builder.process( artifact.getFile() );
                }

            }
            if ( includeJavaHome && javaHome != null && new File( javaHome ).exists() )
            {
                getLog().debug( "Parsing sigantures from " + javaHome );
                process( builder, "lib/rt.jar" );
                process( builder, "lib/jce.jar" );
                process( builder, "lib/jsse.jar" );
            }
            builder.close();
            projectHelper.attachArtifact( project, "signature", classifier, sigFile );

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private static String artifactId( Artifact artifact )
    {
        return ArtifactUtils.artifactId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
                                         artifact.getClassifier(), artifact.getBaseVersion() );
    }

    private ToolchainPrivate[] getToolchains( String type )
        throws MojoExecutionException, MisconfiguredToolchainException
    {
        Class managerClass = toolchainManager.getClass();

        try
        {
            try
            {
                // try 3.x style API
                Method newMethod =
                    managerClass.getMethod( "getToolchainsForType", new Class[]{String.class, MavenSession.class} );

                return (ToolchainPrivate[]) newMethod.invoke( toolchainManager, new Object[]{type, session} );
            }
            catch ( NoSuchMethodException e )
            {
                // try 2.x style API
                Method oldMethod = managerClass.getMethod( "getToolchainsForType", new Class[]{String.class} );

                return (ToolchainPrivate[]) oldMethod.invoke( toolchainManager, new Object[]{type} );
            }
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoExecutionException( "Incompatible toolchain API", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new MojoExecutionException( "Incompatible toolchain API", e );
        }
        catch ( InvocationTargetException e )
        {
            Throwable cause = e.getCause();

            if ( cause instanceof RuntimeException )
            {
                throw (RuntimeException) cause;
            }
            if ( cause instanceof MisconfiguredToolchainException )
            {
                throw (MisconfiguredToolchainException) cause;
            }

            throw new MojoExecutionException( "Incompatible toolchain API", e );
        }
    }


    private void process( SignatureBuilder builder, String name )
        throws IOException
    {
        File f = new File( javaHome, name );
        if ( f.exists() )
        {
            builder.process( f );
        }
    }

    private static File getTargetFile( File basedir, String finalName, String classifier, String type )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + "." + type );
    }

}
