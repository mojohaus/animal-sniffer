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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.animal_sniffer.SignatureBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Stephen Connolly
 * @goal build
 */
public class BuildSignaturesMojo
    extends AbstractMojo
{
    /**
     * The java home to generate the signatures of.
     *
     * @parameter expression="${javaHome}"
     * @required
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
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or
     * deploy it to the local repository instead of the default one in an execution.
     *
     * @parameter expression="${primaryArtifact}" default-value="true"
     */
    private boolean primaryArtifact = true;

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

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File sigFile = getTargetFile( outputDirectory, signaturesName, classifier, "jresig" );
        try
        {
            outputDirectory.mkdirs();
            getLog().info("Parsing signatures from java home: " + javaHome);
            SignatureBuilder builder = new SignatureBuilder( new FileOutputStream( sigFile ) );
            process( builder, "lib/rt.jar" );
            process( builder, "lib/jce.jar" );
            process( builder, "lib/jsse.jar" );
            builder.close();
            String classifier = this.classifier;
            if ( classifier != null )
            {
                projectHelper.attachArtifact( project, "war", classifier, sigFile );
            }
            else
            {
                Artifact artifact = project.getArtifact();
                if ( primaryArtifact )
                {
                    artifact.setFile( sigFile );
                }
                else if ( artifact.getFile() == null || artifact.getFile().isDirectory() )
                {
                    artifact.setFile( sigFile );
                }
            }

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
