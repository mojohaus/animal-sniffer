package org.codehaus.mojo.animal_sniffer.enforcer;

/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.PackageListBuilder;
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

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            List classpathElements = (List) helper.evaluate( "${project.compileClasspathElements}" );

            File outputDirectory = (File) helper.evaluate( "${project.build.outputDirectory}" );

            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );

            MavenProject project = (MavenProject) helper.evaluate( "${project}" );

            ArtifactRepository localRepository = (ArtifactRepository) helper.evaluate( "${localRepository}" );

            ArtifactFactory artifactFactory = (ArtifactFactory) helper.getComponent( ArtifactFactory.class );

            helper.getLog().info( "Checking unresolved references to " + signature );

            org.apache.maven.artifact.Artifact a = signature.createArtifact( artifactFactory );

            resolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );
            // just check code from this module
            final SignatureChecker signatureChecker = new SignatureChecker( new FileInputStream( a.getFile() ),
                                                                            buildPackageList( outputDirectory,
                                                                                              classpathElements ),
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
        PackageListBuilder plb = new PackageListBuilder();
        apply( plb, outputDirectory, classpathElements );
        return plb.packages;
    }

    private void apply( ClassFileVisitor v, File outputDirectory, List classpathElements )
        throws IOException
    {
        v.process( outputDirectory );
        Iterator itr = classpathElements.iterator();
        while ( itr.hasNext() )
        {
            String path = (String) itr.next();
            v.process( new File( path ) );
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
