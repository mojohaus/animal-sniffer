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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
     * Should dependencies be ignored.
     *
     * @parameter default-value="true"
     */
    protected boolean ignoreDependencies;
    
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            List classpathElements = (List) helper.evaluate( "${project.compileClasspathElements}" );

            File outputDirectory = new File( (String) helper.evaluate( "${project.build.outputDirectory}" ) );

            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );

            MavenProject project = (MavenProject) helper.evaluate( "${project}" );

            ArtifactRepository localRepository = (ArtifactRepository) helper.evaluate( "${localRepository}" );

            ArtifactFactory artifactFactory = (ArtifactFactory) helper.getComponent( ArtifactFactory.class );

            helper.getLog().info( "Checking unresolved references to " + signature );

            org.apache.maven.artifact.Artifact a = signature.createArtifact( artifactFactory );

            resolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );
            // just check code from this module

            final Set ignoredPackages = buildPackageList( outputDirectory, classpathElements );

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
                                      new MavenLogger( helper.getLog() ) );
            signatureChecker.process( outputDirectory );

            if ( signatureChecker.isSignatureBroken() )
            {
                throw new EnforcerRuleException(
                    "Signature errors found. Verify them and put @IgnoreJRERequirement on them." );
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

    /**
     * List of packages defined in the application.
     *
     * @param outputDirectory
     */
    private Set buildPackageList( File outputDirectory, List classpathElements )
        throws IOException
    {
        ClassListBuilder plb = new ClassListBuilder();
        apply( plb, outputDirectory, classpathElements );
        return plb.getPackages();
    }

    private void apply( ClassFileVisitor v, File outputDirectory, List classpathElements )
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
}
