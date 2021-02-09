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

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.codehaus.mojo.animal_sniffer.SignatureBuilder;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Generates an API Signature from at least one of: the java runtime, the
 * module dependencies and the module classes.
 *
 * @author Stephen Connolly
 */
@Mojo( name = "build", configurator = "override", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true )
public class BuildSignaturesMojo
    extends AbstractMojo
{
    /**
     * Should the signatures from java home be included.
     *
     * @since 1.3
     */
    @Parameter( property = "includeJavaHome", defaultValue = "true" )
    private boolean includeJavaHome;

    /**
     * Should no signatures be generated if no java home is available.
     *
     * @since 1.3
     */
    @Parameter( property = "skipIfNoJavaHome", defaultValue = "false" )
    private boolean skipIfNoJavaHome;

    /**
     * Should the signatures from this module's classes be included..
     *
     * @since 1.3
     */
    @Parameter( property = "includeJavaHome", defaultValue = "true" )
    private boolean includeModuleClasses;

    /**
     * Classes to generate signatures of.
     *
     * @since 1.3
     */
    @Parameter
    private String[] includeClasses = null;

    /**
     * Classes to exclude from generating signatures of.
     *
     * @since 1.3
     */
    @Parameter
    private String[] excludeClasses = null;

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
     * @since 1.3
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
     * @since 1.3
     */
    @Parameter
    private String[] excludeDependencies = null;

    /**
     * The java home to generate the signatures of, if not specified only the signatures of dependencies will be
     * included. This parameter is overridden by {@link #javaHomeClassPath}.  This parameter overrides {@link #jdk}
     * and any java home specified by maven-toolchains-plugin.
     *
     * @since 1.3
     */
    @Parameter( property = "javaHome" )
    private String javaHome;

    /**
     * Use this configuration option only if the automatic boot classpath detection does not work for the specific
     * {@link #javaHome} or {@link #jdk}.  For example, the automatic boot classpath detection does not work with
     * Sun Java 1.1. This parameter overrides {@link #javaHome}, {@link #jdk} and the maven-toolchains-plugin.
     *
     * @since 1.3
     */
    @Parameter
    private File[] javaHomeClassPath;

    /**
     * Where to put the generated signatures.
     *
     * @since 1.3
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private File outputDirectory;

    /**
     * Where to find this modules classes.
     *
     * @since 1.3
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File classesDirectory;

    /**
     * The name of the generated signatures.
     *
     * @since 1.3
     */
    @Parameter( defaultValue = "${project.build.finalName}", required = true )
    private String signaturesName;

    /**
     * The classifier to add to the generated signatures.
     *
     * @since 1.3
     */
    @Parameter
    private String classifier;

    /**
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The maven project.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;

    /**
     */
    @Component
    private ToolchainManager toolchainManager;

    /**
     */
    @Component
    private ToolchainManagerPrivate toolchainManagerPrivate;

    /**
     * The current build session instance. This is used for toolchain manager API calls.
     */
    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;

    /**
     * The JDK Toolchain to use.  This parameter can be overridden by {@link #javaHome} or {@link #javaHomeClassPath}.
     * This parameter overrides any toolchain specified with maven-toolchains-plugin.
     *
     * @since 1.3
     */
    @Parameter
    private JdkToolchain jdk;

    /**
     */
    @Parameter( defaultValue = "${plugin.artifacts}", required = true, readonly = true )
    private List<Artifact> pluginArtifacts;

    /**
     * The groupId of the Java Boot Classpath Detector to use. The plugin's dependencies will be searched for a
     * dependency of type <code>jar</code> with this groupId and the artifactId specified in {@link #jbcpdArtifactId}.
     * The dependency should be a standalone executable jar file which outputs the java boot classpath as a single
     * line separated using {@link File#pathSeparatorChar} or else exits with a non-zero return code if it cannot determine
     * the java boot classpath.
     *
     * @since 1.3
     */
    @Parameter( defaultValue = "${plugin.groupId}" )
    private String jbcpdGroupId;

    /**
     * The artifactId of the Java Boot Classpath Detector to use. The plugin's dependencies will be searched for a
     * dependency of type <code>jar</code> with this artifactId and the groupId specified in {@link #jbcpdGroupId}.
     * The dependency should be a standalone executable jar file which outputs the java boot classpath as a single
     * line separated using {@link File#pathSeparatorChar} or else exits with a non-zero return code if it cannot determine
     * the java boot classpath.
     *
     * @since 1.3
     */
    @Parameter( defaultValue = "java-boot-classpath-detector" )
    private String jbcpdArtifactId;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( includeJavaHome && ( javaHomeClassPath == null || javaHomeClassPath.length == 0 ) )
        {
            if ( javaHome != null )
            {
                getLog().warn( "Toolchains are ignored, 'javaHome' parameter is set to " + javaHome );

                if ( !new File( javaHome ).isDirectory() )
                {
                    if ( skipIfNoJavaHome )
                    {
                        getLog().warn( "Skipping signature generation as java home (" + javaHome + ") does not exist" );
                        return;
                    }
                    throw new MojoFailureException( "Cannot include java home if specified java home does not exist" );
                }

                if ( !detectJavaBootClasspath( new File( new File( javaHome, "bin" ), "java" ).getAbsolutePath() ) )
                {
                    return;
                }
            }
            else
            {
                Toolchain tc = getJdkToolchain();

                if ( tc == null && jdk == null )
                {
                    String jvm = null;
                    // TODO: how could this give non null result here if tc was null when doing same???
                    tc = toolchainManager.getToolchainFromBuildContext( "jdk", //NOI18N
                                                                        session );
                    getLog().info( "Toolchain from session: " + tc );
                    //assign the path to executable from toolchains
                    if ( tc != null )
                    {
                        jvm = tc.findTool( "java" ); //NOI18N
                    }

                    if ( jvm == null )
                    {
                        if ( skipIfNoJavaHome )
                        {
                            getLog().warn( "Skipping signature generation as could not find java home" );
                            return;
                        }
                        throw new MojoFailureException(
                            "Cannot include java home if java home is not specified (either via javaClassPath, javaHome or jdk)" );
                    }
                    if ( !detectJavaBootClasspath( jvm ) )
                    {
                        return;
                    }
                }
                else if ( tc != null &&  StringUtils.equalsIgnoreCase( "jdk",tc.getType() ) ) //tc instanceof JavaToolChain )
                {
                    getLog().info( "Toolchain in animal-sniffer-maven-plugin: " + tc );

                    //when the executable to use is explicitly set by user in mojo's parameter, ignore toolchains.

                    //assign the path to executable from toolchains
                    String jvm = tc.findTool( "java" ); //NOI18N

                    if ( jvm == null )
                    {
                        if ( skipIfNoJavaHome )
                        {
                            getLog().warn( "Skipping signature generation as could not find java home" );
                            return;
                        }
                        throw new MojoFailureException(
                            "Cannot include java home if java home is not specified (either via javaClassPath, javaHome or jdk)" );
                    }
                    if ( !detectJavaBootClasspath( jvm ) )
                    {
                        return;
                    }
                }
                else if ( tc == null && jdk != null && jdk.getParameters() != null )
                {
                    if ( skipIfNoJavaHome )
                    {
                        getLog().warn( "Skipping signature generation as could not find jdk toolchain to match "
                                           + jdk.getParameters() );
                        return;
                    }
                    throw new MojoFailureException( "Could not find jdk toolchain to match " + jdk.getParameters() );
                }
                else
                {
                    if ( skipIfNoJavaHome )
                    {
                        getLog().warn( "Skipping signature generation as could not find java home" );
                        return;
                    }
                    throw new MojoFailureException(
                        "Cannot include java home if java home is not specified (either via javaClassPath, javaHome, "
                            + "jdk or maven-toolchains-plugin)" );
                }
            }
        }

        displayJavaBootClasspath();

        File sigFile = getTargetFile( outputDirectory, signaturesName, classifier, "signature" );
        try
        {
            outputDirectory.mkdirs();
            SignatureBuilder builder = new SignatureBuilder( getBaseSignatures(), new FileOutputStream( sigFile ),
                                                             new MavenLogger( getLog() ) );

            if ( includeClasses != null )
            {
                getLog().info( "Restricting signatures to include only the following classes:" );
                for ( String includeClass : includeClasses )
                {
                    getLog().info( "  " + includeClass );
                    builder.addInclude( includeClass );
                }
            }

            if ( excludeClasses != null )
            {
                getLog().info( "Restricting signatures to exclude the following classes:" );
                for ( String excludeClass : excludeClasses )
                {
                    getLog().info( "  " + excludeClass );
                    builder.addExclude( excludeClass );
                }
            }

            processJavaBootClasspath( builder );

            processModuleDependencies( builder );

            processModuleClasses( builder );

            builder.close();

            projectHelper.attachArtifact( project, "signature", classifier, sigFile );

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private boolean detectJavaBootClasspath( String javaExecutable )
        throws MojoFailureException, MojoExecutionException
    {
        getLog().info( "Attempting to auto-detect the boot classpath for " + javaExecutable );
        Iterator<Artifact> i = pluginArtifacts.iterator();
        Artifact javaBootClasspathDetector = null;
        while ( i.hasNext() && javaBootClasspathDetector == null )
        {
            Artifact candidate = i.next();

            if ( StringUtils.equals( jbcpdGroupId, candidate.getGroupId() )
                && StringUtils.equals( jbcpdArtifactId, candidate.getArtifactId() ) && candidate.getFile() != null
                && candidate.getFile().isFile() )
            {
                javaBootClasspathDetector = candidate;
            }
        }
        if ( javaBootClasspathDetector == null )
        {
            if ( skipIfNoJavaHome )
            {
                getLog().warn( "Skipping signature generation as could not find boot classpath detector ("
                                   + ArtifactUtils.versionlessKey( jbcpdGroupId, jbcpdArtifactId ) + ")." );
                return false;
            }
            throw new MojoFailureException( "Could not find boot classpath detector ("
                                                + ArtifactUtils.versionlessKey( jbcpdGroupId, jbcpdArtifactId )
                                                + ")." );
        }

        try
        {
            if ( !detectJavaClasspath( javaBootClasspathDetector, javaExecutable ) )
            {
                return false;
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( e.getLocalizedMessage(), e );
        }
        return true;
    }

    private boolean detectJavaClasspath( Artifact javaBootClasspathDetector, String javaExecutable )
        throws CommandLineException, MojoFailureException
    {
        final Commandline cli = new Commandline();
        cli.setWorkingDirectory( project.getBasedir().getAbsolutePath() );
        cli.setExecutable( javaExecutable );
        cli.addEnvironment( "CLASSPATH", "" );
        cli.addEnvironment( "JAVA_HOME", "" );
        cli.addArguments( new String[]{ "-jar", javaBootClasspathDetector.getFile().getAbsolutePath() } );

        final CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();
        final CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
        int exitCode = CommandLineUtils.executeCommandLine( cli, stdout, stderr );
        if ( exitCode != 0 )
        {
            getLog().debug( "Stdout: " + stdout.getOutput() );
            getLog().debug( "Stderr: " + stderr.getOutput() );
            getLog().debug( "Exit code = " + exitCode );
            if ( skipIfNoJavaHome )
            {
                getLog().warn( "Skipping signature generation as could not auto-detect java boot classpath for "
                                   + javaExecutable );
                return false;
            }
            if ( SystemUtils.isJavaVersionAtLeast( JavaVersion.JAVA_9 ))
            {
                getLog().warn( "Skipping signature generation as this java version has no more java boot classpath "
                                   + javaExecutable );
                return false;
            }
            throw new MojoFailureException( "Could not auto-detect java boot classpath for " + javaExecutable );
        }
        String[] classpath = StringUtils.split( stdout.getOutput(), File.pathSeparator );
        javaHomeClassPath = new File[classpath.length];
        for ( int j = 0; j < classpath.length; j++ )
        {
            javaHomeClassPath[j] = new File( classpath[j] );
        }
        return true;
    }

    private void displayJavaBootClasspath()
    {
        if ( includeJavaHome )
        {
            getLog().info( "Java Classpath:" );
            if ( javaHomeClassPath == null )
            {
                getLog().info( "    Empty" );
            }
            else
            {
                for ( int j = 0; j < javaHomeClassPath.length; j++ )
                {
                    getLog().info( "    [" + j + "] = " + javaHomeClassPath[j] );
                }
            }
        }
    }

    private void processModuleDependencies( SignatureBuilder builder )
        throws IOException
    {
        PatternIncludesArtifactFilter includesFilter = includeDependencies == null
            ? null
            : new PatternIncludesArtifactFilter( Arrays.asList( includeDependencies ) );
        PatternExcludesArtifactFilter excludesFilter = excludeDependencies == null
            ? null
            : new PatternExcludesArtifactFilter( Arrays.asList( excludeDependencies ) );

        for ( Artifact artifact : (Iterable<Artifact>) project.getArtifacts() )
        {
            if ( includesFilter != null && !includesFilter.include( artifact ) )
            {
                getLog().debug( "Artifact " + artifactId( artifact ) + " ignored as it does not match include rules." );
                continue;
            }

            if ( excludesFilter != null && !excludesFilter.include( artifact ) )
            {
                getLog().debug( "Artifact " + artifactId( artifact ) + " ignored as it does match exclude rules." );
                continue;
            }

            if ( artifact.getArtifactHandler().isAddedToClasspath() )
            {
                getLog().info( "Parsing signatures from " + artifactId( artifact ) );
                builder.process( artifact.getFile() );
            }

        }
    }

    private void processModuleClasses( SignatureBuilder builder )
        throws IOException
    {
        if ( includeModuleClasses && classesDirectory.isDirectory() )
        {
            getLog().info( "Parsing signatures from " + classesDirectory );
            builder.process( classesDirectory );
        }
    }

    private void processJavaBootClasspath( SignatureBuilder builder )
        throws IOException
    {
        if ( includeJavaHome && javaHomeClassPath != null && javaHomeClassPath.length > 0 )
        {
            getLog().debug( "Parsing signatures java classpath:" );
            for ( File file : javaHomeClassPath )
            {
                if ( file.isFile() || file.isDirectory() )
                {
                    getLog().debug( "Processing " + file );
                    builder.process( file );
                }
                else
                {
                    getLog().warn(
                        "Could not add signatures from boot classpath element: " + file + " as it does not exist." );
                }
            }
        }
    }

    private InputStream[] getBaseSignatures()
        throws FileNotFoundException
    {
        List<InputStream> baseSignatures = new ArrayList<>();
        for ( Artifact artifact : (Iterable<Artifact>) project.getArtifacts() )
        {
            if ( StringUtils.equals( "signature", artifact.getType() ) )
            {
                getLog().info( "Importing sigantures from " + artifact.getFile() );
                baseSignatures.add( new FileInputStream( artifact.getFile() ) );
            }
        }
        return baseSignatures.toArray( new InputStream[0] );
    }

    /**
     * Gets the <code>jdk</code> toolchain to use.
     *
     * @return the <code>jdk</code> toolchain to use or <code>null</code> if no toolchain is configured or if no toolchain can be found.
     * @throws MojoExecutionException if toolchains are misconfigured.
     */
    private Toolchain getJdkToolchain()
        throws MojoExecutionException
    {
        Toolchain tc = getJdkToolchainFromConfiguration();
        if ( tc == null )
        {
            tc = getJdkToolchainFromContext();
        }
        return tc;
    }

    /**
     * Gets the toolchain specified for the current context, e.g. specified via the maven-toolchain-plugin
     *
     * @return the toolchain from the context or <code>null</code> if there is no such toolchain.
     */
    private Toolchain getJdkToolchainFromContext()
    {
        if ( toolchainManager != null )
        {
            return toolchainManager.getToolchainFromBuildContext( "jdk", //NOI18N
                                                                  session );
        }
        return null;
    }

    /**
     * Gets the <code>jdk</code> toolchain from this plugin's configuration.
     *
     * @return the toolchain from this plugin's configuration, or <code>null</code> if no matching toolchain can be
     *         found.
     * @throws MojoExecutionException if the toolchains are configured incorrectly.
     */
    private Toolchain getJdkToolchainFromConfiguration()
        throws MojoExecutionException
    {
        if ( toolchainManager != null && jdk != null && jdk.getParameters() != null )
        {
            try
            {
                final ToolchainPrivate[] tcp = getToolchains( "jdk" );
                for ( ToolchainPrivate toolchainPrivate : tcp )
                {
                    if ( toolchainPrivate.getType().equals( "jdk" ) /* MNG-5716 */
                        && toolchainPrivate.matchesRequirements( jdk.getParameters() ) )
                    {
                        return toolchainPrivate;
                    }
                }
            }
            catch ( MisconfiguredToolchainException e )
            {
                throw new MojoExecutionException( e.getLocalizedMessage(), e );
            }
        }
        return null;
    }

    /*package*/ static String artifactId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType()
                + ( artifact.getClassifier() != null ? ":" + artifact.getClassifier() : "" ) + ":"
                + artifact.getBaseVersion();

    }

    private ToolchainPrivate[] getToolchains( String type )
        throws MojoExecutionException, MisconfiguredToolchainException
    {
        // The toolchain API has moved about quite a bit... this method tries to navigate through to a
        // successful enumeration of all the toolchains of the required type.
        // This method is only ever called in versions of Maven that have toolchain support.

        Class<?> managerClass = toolchainManagerPrivate.getClass();
        try
        {
            try
            {
                // try 3.x style API
                Method newMethod =
                    managerClass.getMethod( "getToolchainsForType", new Class[]{ String.class, MavenSession.class } );

                return (ToolchainPrivate[]) newMethod.invoke( toolchainManagerPrivate, new Object[]{ type, session } );
            }
            catch ( NoSuchMethodException e1 )
            {
                try
                {
                    // try 2.2.1 style API
                    Method oldMethod = managerClass.getMethod( "getToolchainsForType", new Class[]{ String.class } );

                    return (ToolchainPrivate[]) oldMethod.invoke( toolchainManagerPrivate, new Object[]{ type } );
                }
                catch ( NoSuchMethodException e2 )
                {
                    e2.initCause( e1 );
                    throw e2;
                }
            }
        }
        catch ( NoSuchMethodException e )
        {
            StringBuilder buf = new StringBuilder( "Incompatible toolchain API." );
            buf.append( "\n\nCannot find a suitable 'getToolchainsForType' method. Available methods are:\n" );

            Method[] methods = managerClass.getMethods();
            for ( Method method : methods )
            {
                buf.append( "  " ).append( method ).append( '\n' );
            }
            throw new MojoExecutionException( buf.toString(), e );
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
