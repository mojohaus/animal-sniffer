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

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Plexus ConfigurationConverter
 *
 * @author Stephen Connolly
 */
public class JdkToolchainConverter
    extends AbstractConfigurationConverter
{
    public static final String ROLE = ConfigurationConverter.class.getName();

    /**
     * @see org.codehaus.plexus.component.configurator.converters.ConfigurationConverter#canConvert(java.lang.Class)
     */
    public boolean canConvert( Class type )
    {
        return JdkToolchain.class.isAssignableFrom( type );
    }

    /**
     * @see org.codehaus.plexus.component.configurator.converters.ConfigurationConverter#fromConfiguration(org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup, org.codehaus.plexus.configuration.PlexusConfiguration, java.lang.Class, java.lang.Class, java.lang.ClassLoader, org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator, org.codehaus.plexus.component.configurator.ConfigurationListener)
     */
    public Object fromConfiguration( ConverterLookup converterLookup, PlexusConfiguration configuration, Class type,
                                     Class baseType, ClassLoader classLoader, ExpressionEvaluator expressionEvaluator,
                                     ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        return fromConfiguration( configuration, expressionEvaluator );
    }

    private JdkToolchain fromConfiguration( PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws ComponentConfigurationException
    {
        PlexusConfiguration[] params = configuration.getChildren();
        Map parameters = new HashMap();
        for ( int j = 0; j < params.length; j++ )
        {
            try
            {
                String name = params[j].getName();
                String val = params[j].getValue();
                parameters.put( name, val );
            }
            catch ( PlexusConfigurationException ex )
            {
                throw new ComponentConfigurationException( ex );
            }
        }
        final JdkToolchain result = new JdkToolchain();
        result.setParameters( Collections.unmodifiableMap( parameters ) );
        return result;
    }
}
