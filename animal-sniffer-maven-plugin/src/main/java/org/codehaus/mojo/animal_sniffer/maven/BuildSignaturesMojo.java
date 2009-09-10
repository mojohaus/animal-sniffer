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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.mojo.animal_sniffer.SignatureBuilder;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Stephen Connolly
 * @goal build
 */
public class BuildSignaturesMojo
    extends AbstractMojo
{
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

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        //get toolchain from context
        Toolchain tc = toolchainManager.getToolchainFromBuildContext( "jdk", //NOI18N
                                                                      session );
        if ( tc != null )
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
                javaHome = tc.findTool( "jdkHome" ); //NOI18N
            }
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
            if (classesDirectory.isDirectory()) {
                getLog().info( "Parsing sigantures from " + classesDirectory );
                builder.process( classesDirectory );                
            }
            for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                if ( StringUtils.equals( "jar", artifact.getType() ) )
                {
                    getLog().info( "Parsing sigantures from " + artifact.getFile() );
                    builder.process( artifact.getFile() );
                }

            }
            if ( javaHome != null && new File( javaHome ).exists() )
            {
                getLog().debug( "Parsing sigantures from " + javaHome);
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
