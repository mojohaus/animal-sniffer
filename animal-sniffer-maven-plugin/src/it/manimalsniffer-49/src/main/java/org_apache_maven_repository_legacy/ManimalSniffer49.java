package org_apache_maven_repository_legacy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Properties;

/**
 * Critical code from org.apache.maven.repository.legacy.DefaultUpdateCheckManager
 * 
 * 
 * @author Robert Scholte
 *
 */
public class ManimalSniffer49
{
    private static final String ERROR_KEY_SUFFIX = ".error";

    void writeLastUpdated( File touchfile, String key, String error )
    {
        synchronized ( touchfile.getAbsolutePath().intern() )
        {
            if ( !touchfile.getParentFile().exists() && !touchfile.getParentFile().mkdirs() )
            {
//                getLogger().debug( "Failed to create directory: " + touchfile.getParent()
//                                       + " for tracking artifact metadata resolution." );
                return;
            }

            FileChannel channel = null;
            FileLock lock = null;
            try
            {
                Properties props = new Properties();

                channel = new RandomAccessFile( touchfile, "rw" ).getChannel();
                lock = channel.lock( 0, channel.size(), false );

                if ( touchfile.canRead() )
                {
//                    getLogger().debug( "Reading resolution-state from: " + touchfile );
                    ByteBuffer buffer = ByteBuffer.allocate( (int) channel.size() );

                    channel.read( buffer );
                    buffer.flip();

                    ByteArrayInputStream stream = new ByteArrayInputStream( buffer.array() );
                    props.load( stream );
                }

                props.setProperty( key, Long.toString( System.currentTimeMillis() ) );

                if ( error != null )
                {
                    props.setProperty( key + ERROR_KEY_SUFFIX, error );
                }
                else
                {
                    props.remove( key + ERROR_KEY_SUFFIX );
                }

                ByteArrayOutputStream stream = new ByteArrayOutputStream();

//                getLogger().debug( "Writing resolution-state to: " + touchfile );
                props.store( stream, "Last modified on: " + new Date() );

                byte[] data = stream.toByteArray();
                ByteBuffer buffer = ByteBuffer.allocate( data.length );
                buffer.put( data );
                buffer.flip();

                channel.position( 0 );
                channel.write( buffer );
            }
            catch ( IOException e )
            {
//                getLogger().debug( "Failed to record lastUpdated information for resolution.\nFile: "
//                                       + touchfile.toString() + "; key: " + key, e );
            }
            finally
            {
                if ( lock != null )
                {
                    try
                    {
                        lock.release();
                    }
                    catch ( IOException e )
                    {
//                        getLogger().debug( "Error releasing exclusive lock for resolution tracking file: "
//                                               + touchfile, e );
                    }
                }

                if ( channel != null )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( IOException e )
                    {
//                        getLogger().debug( "Error closing FileChannel for resolution tracking file: "
//                                               + touchfile, e );
                    }
                }
            }
        }
    }
    
}
